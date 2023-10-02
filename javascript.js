function getXPosition(el) {return el.getBoundingClientRect().left;}
function getYPosition(el) {return el.getBoundingClientRect().top+window.scrollY;}
function getMaxWidth(el) {return el.getBoundingClientRect().width;}
function getMaxHeight(el) {return el.getBoundingClientRect().height;}
function elementIsVisible(el) {if (getComputedStyle(el).visibility === 'hidden' || el.getBoundingClientRect().height == 0) return false; return true;}
function getXPath(element) {const idx = (sib, name) => sib ? idx(sib.previousElementSibling, name||sib.localName) + (sib.localName == name): 1; const segs = elm => !elm || elm.nodeType !== 1 ? ['']: [...segs(elm.parentNode), elm instanceof HTMLElement? `${elm.localName}[${idx(elm)}]`: `*[local-name() = "${elm.localName}"][${idx(elm)}]`]; return segs(element).join('/');}
function getIdXPath(element) {const idx = (sib, name) => sib ? idx(sib.previousElementSibling, name||sib.localName) + (sib.localName == name): 1; const segs = elm => !elm || elm.nodeType !== 1 ? ['']: elm.id && document.getElementById(elm.id) === elm ? [`//*[@id='${elm.id}']`]: [...segs(elm.parentNode), elm instanceof HTMLElement? `${elm.localName}[${idx(elm)}]`: `*[local-name() = "${elm.localName}"][${idx(elm)}]`]; return segs(element).join('/');}
