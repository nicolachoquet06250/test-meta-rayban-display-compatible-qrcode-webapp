export type QRKind = 'empty' | 'error' | 'wifi' | 'contact' | 'email' | 'phone' | 'url' | 'text';
export interface QRResult { kind: QRKind; title: string; details: [string, string][]; raw?: string; action?: string; secure?: boolean }
const clean = (value: unknown): string => String(value ?? '').trim();
const parts = (value: string) => value.split(/(?<!\\);/).filter(Boolean);
const unescapeWifi = (value: string) => value.replace(/\\([\\;,:])/g, '$1');
export function parseQR(raw: unknown): QRResult {
  const text = clean(raw);
  if (!text) return { kind: 'empty', title: 'Aucune donnée', details: [] };
  if (text.length > 8192) return { kind: 'error', title: 'Contenu trop long', details: [] };
  if (/^WIFI:/i.test(text)) {
    const fields: Record<string, string> = {};
    for (const segment of parts(text.slice(5).replace(/;$/, ''))) {
      const match = segment.match(/^([STPH]):(.*)$/i);
      if (match?.[1] && match[2] !== undefined) fields[match[1].toUpperCase()] = unescapeWifi(match[2]);
    }
    return {kind:'wifi',title:fields.S || 'Réseau Wi-Fi',details:[['Sécurité',fields.T || 'Non précisée'],['Mot de passe',fields.P ? '••••••••' : 'Absent'],['Réseau masqué',fields.H === 'true' ? 'Oui' : 'Non']],raw:text};
  }
  if (/^BEGIN:VCARD/i.test(text)) {
    const read = (key: string) => (text.match(new RegExp('^'+key+'(?:;[^:]*)?:(.*)$','im'))?.[1] || '').trim();
    const name = read('FN') || read('N') || 'Contact';
    return {kind:'contact',title:name,details:[['Téléphone',read('TEL')],['E-mail',read('EMAIL')],['Organisation',read('ORG')]].filter((x): x is [string, string] => Boolean(x[1])),raw:text};
  }
  if (/^mailto:/i.test(text)) {
    try { const u = new URL(text); return {kind:'email',title:decodeURIComponent(u.pathname),details:[['Objet',u.searchParams.get('subject') || 'Non précisé']],action:text,raw:text}; } catch {}
  }
  if (/^tel:/i.test(text)) return {kind:'phone',title:text.slice(4),details:[],action:text,raw:text};
  if (/^https?:\/\//i.test(text)) {
    try {
      const u = new URL(text);
      if (u.protocol === 'https:' || u.protocol === 'http:') return {kind:'url',title:u.hostname,details:[['Adresse',u.href]],action:u.href,raw:text,secure:u.protocol === 'https:'};
    } catch {}
  }
  return {kind:'text',title:'Texte',details:[['Contenu',text]],raw:text};
}
