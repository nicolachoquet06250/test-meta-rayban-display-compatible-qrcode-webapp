import { parseQR, type QRResult, type QRKind } from './parser';
import './style.css';

const root = document.querySelector<HTMLElement>('#app');
if (!root) throw new Error('Conteneur #app introuvable');
let result: QRResult | null = null;
let value = '';

type ElementOptions = { className?: string; text?: string; type?: string; href?: string; placeholder?: string; ariaLabel?: string };
function node<K extends keyof HTMLElementTagNameMap>(tag: K, options: ElementOptions = {}, children: Node[] = []): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag);
  if (options.className) el.className = options.className;
  if (options.text !== undefined) el.textContent = options.text;
  if (options.type && (el instanceof HTMLButtonElement || el instanceof HTMLInputElement)) el.type = options.type;
  if (options.href && el instanceof HTMLAnchorElement) el.href = options.href;
  if (options.placeholder && el instanceof HTMLTextAreaElement) el.placeholder = options.placeholder;
  if (options.ariaLabel) el.setAttribute('aria-label', options.ariaLabel);
  for (const child of children) el.append(child);
  return el;
}
function button(text: string, handler: () => void, className = ''): HTMLButtonElement {
  const b = node('button', { text, className, type: 'button' });
  b.addEventListener('click', handler);
  return b;
}
function setData(input: unknown): void {
  value = String(input ?? '');
  result = parseQR(value);
  render();
}
function render(): void {
  root!.replaceChildren();
  const header = node('header',{},[
    node('div',{className:'brand',text:'▦ QR LENS'}),
    node('div',{className:'label',text:'META WEARABLES · EXTERNAL INPUT'})
  ]);
  const body = node('section',{className:'content'});
  if (!result || result.kind === 'empty') {
    body.append(node('div',{className:'symbol',text:'▦'}),node('h1',{text:'Lire un QR code'}),node('p',{className:'intro',text:'Recevez un contenu QR depuis une URL ou saisissez-le ici.'}));
  } else {
    body.append(node('div',{className:'eyebrow',text:({empty:'',url:'LIEN WEB',wifi:'RÉSEAU WI-FI',contact:'CONTACT',email:'E-MAIL',phone:'TÉLÉPHONE',text:'CONTENU TEXTE',error:'ERREUR'} satisfies Record<QRKind, string>)[result.kind]}),node('h1',{text:result.title}));
    const dl = node('dl',{className:'details'});
    for (const [key,val] of result.details) dl.append(node('dt',{text:key}),node('dd',{text:val}));
    body.append(dl);
    if (result.kind === 'url' && !result.secure) body.append(node('p',{className:'warning',text:'Lien HTTP non chiffré — ouverture désactivée.'}));
  }
  const footer = node('footer');
  const field = node('textarea',{className:'entry',placeholder:'Coller un texte, une URL ou WIFI:…',ariaLabel:'Contenu du QR code'});
  field.value = value;
  footer.append(field);
  const actions = node('div',{className:'actions'});
  actions.append(button('Analyser',()=>setData(field.value),'primary'));
  if (result && result.kind !== 'empty') {
    actions.append(button('Effacer',()=>setData('')));
    if (navigator.clipboard?.writeText && result.raw !== undefined) {
      const raw = result.raw;
      actions.append(button('Copier', async () => {
        try { await navigator.clipboard.writeText(raw); status.textContent = 'Données copiées'; }
        catch { status.textContent = 'Copie impossible'; }
      }));
    }
    if (result.kind === 'url' && result.secure && result.action) {
      const link = node('a',{text:'Ouvrir ↗',href:result.action,className:'button external'});
      link.target='_blank'; link.rel='noopener noreferrer'; actions.append(link);
    }
    if ((result.kind === 'email' || result.kind === 'phone') && result.action) {
      const link=node('a',{text:'Utiliser ↗',href:result.action,className:'button external'});
      actions.append(link);
    }
  }
  const status = node('p',{className:'status',text:'Navigation : commandes directionnelles des lunettes'});
  footer.append(actions,status);
  root!.append(header,body,footer);
}
const params = new URLSearchParams(window.location.search);
const data = params.get('data');
if (data !== null) {
  setData(data);
  // Remove potentially private QR payloads from the displayed URL and browser history entry.
  history.replaceState(null,'',window.location.pathname + window.location.hash);
} else render();
