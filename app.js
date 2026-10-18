(() => {
  const $ = (sel, root = document) => root.querySelector(sel);
  const CHECK_VALUE = 'id-vault-ok';
  const HIDDEN_LOCK_MS = 60 * 1000;
  const MAX_IMAGE_SIDE = 2400;
  const SOON_DAYS = 90;

  const state = {
    key: null,
    items: [],          // [{ id, fields }]
    urls: new Map(),    // image id -> object URL (revoked on lock)
    current: null,      // item id shown in detail / being edited
    editType: null,     // type key while editing
    draftImages: {},    // side -> Blob | null (null = removed)
    reveal: false,
  };

  // ---------- Small helpers ----------

  function el(tag, props = {}, ...children) {
    const n = document.createElement(tag);
    for (const [k, v] of Object.entries(props)) {
      if (k === 'vars') for (const [vk, vv] of Object.entries(v)) n.style.setProperty(vk, vv);
      else n[k] = v;
    }
    for (const c of children) if (c != null && c !== false) n.append(c);
    return n;
  }

  function icon(name, cls = '') {
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('class', `i ${cls}`.trim());
    const use = document.createElementNS(ns, 'use');
    use.setAttribute('href', `#i-${name}`);
    svg.append(use);
    return svg;
  }

  let toastTimer;
  function toast(msg) {
    const t = $('#toast');
    t.textContent = msg;
    t.hidden = false;
    t.classList.remove('show');
    void t.offsetWidth;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (t.hidden = true), 2400);
  }

  function show(id) {
    for (const s of document.querySelectorAll('.screen')) {
      const on = s.id === id;
      if (on && s.hidden) {
        s.classList.remove('enter');
        void s.offsetWidth;
        s.classList.add('enter');
      }
      s.hidden = !on;
    }
    window.scrollTo(0, 0);
  }

  function daysUntil(dateStr) {
    if (!dateStr) return null;
    const d = new Date(dateStr + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.round((d - today) / 86400000);
  }

  function expiryStatus(fields) {
    const days = daysUntil(fields.expires);
    if (days == null) return null;
    if (days < 0) return { level: 'bad', text: 'Expired', days };
    if (days <= SOON_DAYS) return { level: 'warn', text: days === 0 ? 'Expires today' : `Expires in ${days} days`, days };
    return null;
  }

  const fmtDate = (s) => (s ? new Date(s + 'T00:00:00').toLocaleDateString('pt-BR') : '');
  const fmtMonth = (s) => (s ? new Date(s + 'T00:00:00').toLocaleDateString('pt-BR', { month: '2-digit', year: '2-digit' }) : '');

  // Keep the last 3 characters visible, hide the rest of the letters/digits.
  function mask(v) {
    const s = String(v);
    let keep = 3;
    let out = '';
    for (let i = s.length - 1; i >= 0; i--) {
      const ch = s[i];
      if (/[0-9A-Za-z]/.test(ch)) out = (keep-- > 0 ? ch : '•') + out;
      else out = ch + out;
    }
    return out;
  }

  // ---------- Sheet dialog (replaces alert/confirm/prompt) ----------

  function ask({ title, text = '', inputs = [], ok = 'OK', danger = false }) {
    return new Promise((resolve) => {
      $('#sheet-title').textContent = title;
      $('#sheet-text').textContent = text;
      $('#sheet-text').hidden = !text;
      const fields = inputs.map((inp) => el('input', { className: 'sheet-input', autocomplete: 'off', ...inp }));
      $('#sheet-inputs').replaceChildren(...fields);
      const okBtn = $('#sheet-ok');
      okBtn.textContent = ok;
      okBtn.classList.toggle('danger-fill', danger);
      $('#sheet').hidden = false;
      (fields[0] || okBtn).focus();
      const done = (val) => {
        $('#sheet').hidden = true;
        $('#sheet-form').onsubmit = null;
        $('#sheet-cancel').onclick = null;
        resolve(val);
      };
      $('#sheet-form').onsubmit = (e) => {
        e.preventDefault();
        done(inputs.length ? fields.map((f) => f.value) : true);
      };
      $('#sheet-cancel').onclick = () => done(null);
    });
  }

  // ---------- Card rendering ----------

  function primaryValue(fields) {
    const t = DocTypes.get(fields.type);
    const f = t.fields.find((x) => x.primary);
    return f ? { field: f, value: fields[f.k] || '' } : { field: null, value: '' };
  }

  function renderCard(fields, { reveal = false, compact = false } = {}) {
    const t = DocTypes.get(fields.type);
    const { field, value } = primaryValue(fields);
    const status = expiryStatus(fields);
    const shown = value ? (field.secret && !reveal ? mask(value) : value) : '—';
    const sub = t.key === 'other' ? fields.issuer || t.sub : t.key === 'council' && fields.council ? `${t.name}${fields.uf ? ' · ' + fields.uf : ''}` : t.name;
    const rightMeta = fields.category
      ? { label: 'CAT', value: fields.category }
      : fields.expires
        ? { label: 'VALID', value: fmtMonth(fields.expires) }
        : fields.uf
          ? { label: 'UF', value: fields.uf }
          : null;

    return el(
      'div',
      {
        className: `idcard${t.gold ? ' gold' : ''}${compact ? ' compact' : ''}`,
        vars: { '--c1': t.c1, '--c2': t.c2, '--ink': t.ink },
      },
      el('div', { className: 'idc-sheen' }),
      el(
        'div',
        { className: 'idc-top' },
        el('div', { className: 'idc-titles' },
          el('div', { className: 'idc-abbr', textContent: DocTypes.title(fields) }),
          el('div', { className: 'idc-name', textContent: sub })),
        el('div', { className: 'idc-country' },
          el('span', { className: 'idc-flag' }),
          el('span', { textContent: t.key === 'passport' && fields.country ? fields.country.toUpperCase() : 'BRASIL' }))
      ),
      !compact && el('div', { className: 'idc-mid' },
        icon('chip', 'idc-chip'),
        status && el('span', { className: `idc-status ${status.level}`, textContent: status.level === 'bad' ? 'EXPIRED' : 'EXPIRING' })),
      !compact && el('div', { className: 'idc-number', textContent: shown }),
      !compact && el(
        'div',
        { className: 'idc-bottom' },
        el('div', { className: 'idc-holder' },
          el('small', { textContent: t.key === 'crlv' ? 'OWNER' : 'HOLDER' }),
          el('span', { textContent: (fields.holder || '—').toUpperCase() })),
        rightMeta && el('div', { className: 'idc-meta' },
          el('small', { textContent: rightMeta.label }),
          el('span', { textContent: rightMeta.value }))
      )
    );
  }

  // ---------- Lock / unlock ----------

  async function initLock() {
    const meta = await VaultDB.get('meta', 'vault');
    const setup = !meta;
    $('#pass2-wrap').hidden = !setup;
    $('#pass2').required = setup;
    $('#pass').autocomplete = setup ? 'new-password' : 'current-password';
    $('#lock-btn').textContent = setup ? 'Create my vault' : 'Unlock';
    $('#lock-hint').textContent = setup
      ? 'Create a passphrase (8+ characters). It encrypts everything and can’t be recovered if lost.'
      : 'Enter your passphrase to unlock your documents.';
    $('#lock-error').textContent = '';
    $('#pass').value = '';
    $('#pass2').value = '';
    show('lock');
    $('#pass').focus();
  }

  async function onLockSubmit(e) {
    e.preventDefault();
    const pass = $('#pass').value;
    const btn = $('#lock-btn');
    const label = btn.textContent;
    $('#lock-error').textContent = '';
    btn.disabled = true;
    btn.textContent = 'Decrypting…';
    try {
      let meta = await VaultDB.get('meta', 'vault');
      if (!meta) {
        if (pass.length < 8) throw new Error('Use at least 8 characters.');
        if (pass !== $('#pass2').value) throw new Error('Passphrases don’t match.');
        const salt = VaultCrypto.randomBytes(16);
        const key = await VaultCrypto.deriveKey(pass, salt);
        const check = await VaultCrypto.encryptJSON(key, CHECK_VALUE);
        meta = { k: 'vault', salt, iterations: VaultCrypto.ITERATIONS, check };
        await VaultDB.put('meta', meta);
        state.key = key;
      } else {
        const key = await VaultCrypto.deriveKey(pass, meta.salt, meta.iterations);
        try {
          if ((await VaultCrypto.decryptJSON(key, meta.check)) !== CHECK_VALUE) throw 0;
        } catch {
          throw new Error('Wrong passphrase.');
        }
        state.key = key;
      }
      await loadItems();
      history.replaceState({ screen: 'main' }, '');
      renderList();
      show('main');
      resetIdle();
    } catch (err) {
      $('#lock-error').textContent = err.message || String(err);
      const card = $('.lock-card');
      card.classList.remove('shake');
      void card.offsetWidth;
      card.classList.add('shake');
    } finally {
      btn.disabled = false;
      btn.textContent = label;
      $('#pass').value = '';
      $('#pass2').value = '';
    }
  }

  function lock() {
    state.key = null;
    state.items = [];
    state.current = null;
    state.draftImages = {};
    state.reveal = false;
    for (const u of state.urls.values()) URL.revokeObjectURL(u);
    state.urls.clear();
    for (const id of ['#stack', '#d-card', '#d-photos', '#d-fields', '#e-preview', '#e-fields', '#alerts']) $(id).replaceChildren();
    $('#sheet').hidden = true;
    closeViewer();
    initLock();
  }

  function autoLockMinutes() {
    try { return Number(localStorage.getItem('autolock')) || 5; } catch { return 5; }
  }

  let idleTimer;
  function resetIdle() {
    clearTimeout(idleTimer);
    if (state.key) idleTimer = setTimeout(lock, autoLockMinutes() * 60000);
  }

  let hiddenAt = 0;
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) hiddenAt = Date.now();
    else if (state.key && Date.now() - hiddenAt > HIDDEN_LOCK_MS) lock();
  });
  for (const ev of ['pointerdown', 'keydown', 'scroll']) document.addEventListener(ev, resetIdle, { passive: true });

  // ---------- Data ----------

  async function loadItems() {
    const rows = await VaultDB.getAll('items');
    state.items = await Promise.all(
      rows.map(async (r) => ({ id: r.id, fields: DocTypes.normalize(await VaultCrypto.decryptJSON(state.key, r.box)) }))
    );
  }

  async function imageBlob(id) {
    const row = await VaultDB.get('images', id);
    if (!row) return null;
    const bytes = await VaultCrypto.decryptBytes(state.key, row.box);
    return new Blob([bytes], { type: row.type || 'image/jpeg' });
  }

  async function imageURL(id) {
    if (state.urls.has(id)) return state.urls.get(id);
    const blob = await imageBlob(id);
    if (!blob) return null;
    const url = URL.createObjectURL(blob);
    state.urls.set(id, url);
    return url;
  }

  function dropURL(id) {
    const u = state.urls.get(id);
    if (u) URL.revokeObjectURL(u);
    state.urls.delete(id);
  }

  async function saveImage(itemId, side, blob) {
    const id = `${itemId}:${side}`;
    dropURL(id);
    if (!blob) return VaultDB.del('images', id);
    const box = await VaultCrypto.encryptBytes(state.key, await blob.arrayBuffer());
    await VaultDB.put('images', { id, box, type: blob.type });
  }

  // Downscale very large photos so the vault stays fast; screenshots are usually kept as-is.
  async function prepareImage(file) {
    const bmp = await createImageBitmap(file);
    const scale = Math.min(1, MAX_IMAGE_SIDE / Math.max(bmp.width, bmp.height));
    if (scale === 1 && file.size < 4 * 1024 * 1024 && /^image\/(png|jpeg|webp)$/.test(file.type)) {
      bmp.close();
      return file;
    }
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bmp.width * scale);
    canvas.height = Math.round(bmp.height * scale);
    canvas.getContext('2d').drawImage(bmp, 0, 0, canvas.width, canvas.height);
    bmp.close();
    return new Promise((res) => canvas.toBlob(res, 'image/jpeg', 0.92));
  }

  const SEARCH_KEYS = (f) => [DocTypes.title(f), DocTypes.get(f.type).name, ...Object.values(f)];

  function sortedItems() {
    const order = Object.fromEntries(DocTypes.list.map((t, i) => [t.key, i]));
    return [...state.items].sort((a, b) => order[a.fields.type] - order[b.fields.type] || (a.fields.holder || '').localeCompare(b.fields.holder || ''));
  }

  // ---------- Wallet ----------

  function renderList() {
    const q = $('#search').value.trim().toLowerCase();
    const items = sortedItems().filter((i) => !q || SEARCH_KEYS(i.fields).some((v) => String(v || '').toLowerCase().includes(q)));
    const n = state.items.length;
    $('#count').textContent = n ? `${n} document${n === 1 ? '' : 's'} · encrypted` : 'Wallet';

    const attention = state.items.map((i) => ({ i, s: expiryStatus(i.fields) })).filter((x) => x.s).sort((a, b) => a.s.days - b.s.days);
    $('#alerts').replaceChildren(
      ...(q ? [] : attention.slice(0, 3)).map(({ i, s }) => {
        const b = el('button', { className: `alert ${s.level}` },
          icon('alert', 'sm'),
          el('span', {}, el('b', { textContent: DocTypes.title(i.fields) }), ` · ${s.text}`),
          icon('chevron', 'sm chev'));
        b.onclick = () => openDetail(i.id);
        return b;
      })
    );

    const stack = $('#stack');
    stack.classList.toggle('flat', !!q || items.length <= 1);
    stack.replaceChildren(
      ...items.map((item, idx) => {
        const card = renderCard(item.fields);
        card.tabIndex = 0;
        card.setAttribute('role', 'button');
        card.setAttribute('aria-label', `${DocTypes.title(item.fields)} ${item.fields.holder || ''}`);
        card.style.setProperty('--i', idx);
        card.onclick = () => openDetail(item.id);
        card.onkeydown = (e) => { if (e.key === 'Enter') openDetail(item.id); };
        return card;
      })
    );
    if (q && !items.length) stack.append(el('p', { className: 'no-results', textContent: `No documents match “${$('#search').value.trim()}”` }));
    $('#empty').hidden = n > 0;
    $('#search').parentElement.hidden = n === 0;
    $('#add-btn').hidden = n === 0;
  }

  // ---------- Detail ----------

  async function openDetail(id, push = true) {
    const item = state.items.find((i) => i.id === id);
    if (!item) return goMain();
    if (state.current !== id) state.reveal = false;
    state.current = id;
    const f = item.fields;
    const t = DocTypes.get(f.type);
    $('#d-title').textContent = DocTypes.title(f);
    $('#d-card').replaceChildren(renderCard(f, { reveal: state.reveal }));

    const hasSecret = t.fields.some((x) => x.secret && f[x.k]);
    $('#a-reveal').hidden = !hasSecret;
    $('#a-reveal use').setAttribute('href', state.reveal ? '#i-eye-off' : '#i-eye');
    $('#a-reveal em').textContent = state.reveal ? 'Hide' : 'Reveal';
    $('#a-copy').disabled = !primaryValue(f).value;

    const photos = [];
    for (const side of ['front', 'back']) {
      const u = await imageURL(`${id}:${side}`);
      if (!u) continue;
      const b = el('button', { className: 'photo' }, el('img', { src: u, alt: `${side} of ${DocTypes.title(f)}` }), el('span', { textContent: side === 'front' ? 'Front' : 'Back' }));
      b.onclick = () => openViewer(u);
      photos.push(b);
    }
    $('#d-photos').replaceChildren(...photos);
    $('#d-photos-wrap').hidden = !photos.length;
    $('#a-photo').disabled = !photos.length;
    $('#a-share').disabled = !photos.length && !primaryValue(f).value;

    const rows = [];
    for (const fd of t.fields) {
      const raw = f[fd.k];
      if (!raw) continue;
      const display = fd.kind === 'date' ? fmtDate(raw) : fd.secret && !state.reveal ? mask(raw) : raw;
      const value = el('div', { className: `fv${fd.mono ? ' mono' : ''}`, textContent: display });
      if (fd.k === 'expires') {
        const s = expiryStatus(f);
        if (s) value.append(el('span', { className: `pill ${s.level}`, textContent: s.text }));
      }
      const row = el('div', { className: 'frow' }, el('div', { className: 'fl', textContent: fd.label }), value);
      if (fd.mono || fd.kind === 'cpf') {
        const c = el('button', { className: 'mini-btn', ariaLabel: `Copy ${fd.label}` }, icon('copy', 'sm'));
        c.onclick = () => copy(raw, fd.label);
        row.append(c);
      }
      rows.push(row);
    }
    if (f.notes) rows.push(el('div', { className: 'frow' }, el('div', { className: 'fl', textContent: 'Notes' }), el('div', { className: 'fv notes', textContent: f.notes })));
    if (!rows.length) rows.push(el('div', { className: 'frow' }, el('div', { className: 'fv muted', textContent: 'No details yet. Tap Edit to add some.' })));
    $('#d-fields').replaceChildren(...rows);

    show('detail');
    if (push) history.pushState({ screen: 'detail', id }, '');
  }

  async function copy(text, what = 'Number') {
    try {
      await navigator.clipboard.writeText(text);
      toast(`${what} copied`);
    } catch {
      toast('Copy isn’t available here');
    }
  }

  async function shareCurrent() {
    const item = state.items.find((i) => i.id === state.current);
    if (!item) return;
    const name = DocTypes.title(item.fields);
    const blob = await imageBlob(`${item.id}:front`);
    if (blob) {
      const ext = blob.type.split('/')[1] || 'jpg';
      const file = new File([blob], `${name}.${ext}`, { type: blob.type });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        try { await navigator.share({ files: [file], title: name }); } catch { /* cancelled */ }
        return;
      }
      const a = el('a', { href: URL.createObjectURL(file), download: file.name });
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
      return toast('Photo saved to downloads');
    }
    const { value } = primaryValue(item.fields);
    if (navigator.share) {
      try { await navigator.share({ title: name, text: `${name}: ${value}` }); } catch { /* cancelled */ }
    } else copy(value);
  }

  function openViewer(url) {
    $('#viewer img').src = url;
    $('#viewer').classList.remove('zoom');
    $('#viewer').hidden = false;
  }
  function closeViewer() {
    $('#viewer').hidden = true;
    $('#viewer img').removeAttribute('src');
  }

  // ---------- Type picker ----------

  function renderPicker() {
    $('#type-grid').replaceChildren(
      ...DocTypes.list.map((t) => {
        const b = el('button', { className: 'type-tile', vars: { '--c1': t.c1, '--c2': t.c2, '--ink': t.ink } },
          el('span', { className: `tile-swatch${t.gold ? ' gold' : ''}` }, el('b', { textContent: t.abbr })),
          el('span', { className: 'tile-text' }, el('b', { textContent: t.key === 'other' ? 'Other' : t.abbr }), el('small', { textContent: t.key === 'other' ? t.sub : t.name })));
        b.onclick = () => openEdit(null, t.key, 'replace');
        return b;
      })
    );
  }

  function openPicker() {
    renderPicker();
    show('picker');
    history.pushState({ screen: 'picker' }, '');
  }

  // ---------- Edit ----------

  function collectFields() {
    const form = $('#edit-form');
    const out = { type: state.editType };
    for (const elx of form.elements) if (elx.name) out[elx.name] = elx.value.trim();
    return out;
  }

  function updatePreview() {
    $('#e-preview').replaceChildren(renderCard(collectFields(), { reveal: true }));
  }

  function buildField(fd, value = '') {
    let input;
    if (fd.kind === 'select') {
      input = el('select', { name: fd.k }, ...fd.opts.map((o) => el('option', { value: o, textContent: o || 'Select…' })));
      if (value && !fd.opts.includes(value)) input.append(el('option', { value, textContent: value }));
    } else if (fd.kind === 'textarea') {
      input = el('textarea', { name: fd.k, rows: 2 });
    } else {
      input = el('input', { name: fd.k, type: fd.kind === 'date' ? 'date' : 'text', placeholder: fd.placeholder || '' });
      if (fd.kind === 'cpf') {
        input.inputMode = 'numeric';
        input.placeholder = '000.000.000-00';
        input.addEventListener('input', () => (input.value = DocTypes.formatCPF(input.value)));
      }
      if (fd.mono) input.classList.add('mono');
      if (fd.k === 'holder') input.autocapitalize = 'words';
    }
    if (fd.required) input.required = true;
    input.value = value;
    return el('label', { className: 'field' }, el('span', { textContent: fd.label }), input);
  }

  async function openEdit(id, typeKey, mode = 'push') {
    const item = id && state.items.find((i) => i.id === id);
    state.current = id || null;
    state.editType = item ? item.fields.type : typeKey;
    state.draftImages = {};
    const t = DocTypes.get(state.editType);
    const f = item ? item.fields : { type: t.key };
    $('#e-title').textContent = item ? `Edit ${DocTypes.title(f)}` : `New ${t.key === 'other' ? 'document' : t.abbr}`;
    $('#e-fields').replaceChildren(...t.fields.map((fd) => buildField(fd, f[fd.k] || '')));
    $('#edit-form').elements.notes.value = f.notes || '';
    for (const pick of document.querySelectorAll('.img-pick')) {
      setPickPreview(pick, item ? await imageURL(`${id}:${pick.dataset.side}`) : null);
    }
    updatePreview();
    show('edit');
    const st = { screen: 'edit', id, type: state.editType };
    if (mode === 'replace') history.replaceState(st, '');
    else if (mode === 'push') history.pushState(st, '');
  }

  function setPickPreview(pick, url) {
    const img = $('img', pick);
    img.hidden = !url;
    if (url) img.src = url;
    else img.removeAttribute('src');
    $('.pick-empty', pick).hidden = !!url;
    $('.clear-img', pick).hidden = !url;
    pick.classList.toggle('filled', !!url);
  }

  const draftURLs = [];
  function setupPickers() {
    for (const pick of document.querySelectorAll('.img-pick')) {
      const side = pick.dataset.side;
      $('input', pick).addEventListener('change', async (e) => {
        const file = e.target.files[0];
        e.target.value = '';
        if (!file) return;
        try {
          const blob = await prepareImage(file);
          state.draftImages[side] = blob;
          const u = URL.createObjectURL(blob);
          draftURLs.push(u);
          setPickPreview(pick, u);
        } catch {
          toast('Couldn’t read that image');
        }
      });
      $('.clear-img', pick).addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        state.draftImages[side] = null;
        setPickPreview(pick, null);
      });
    }
  }

  async function saveEdit() {
    const form = $('#edit-form');
    if (!form.reportValidity()) return;
    const btn = $('#e-save');
    btn.disabled = true;
    try {
      const isNew = !state.current;
      const id = state.current || crypto.randomUUID();
      const existing = state.items.find((i) => i.id === id);
      const fields = { ...(existing ? existing.fields : {}), ...collectFields() };
      await VaultDB.put('items', { id, box: await VaultCrypto.encryptJSON(state.key, fields) });
      for (const [side, blob] of Object.entries(state.draftImages)) await saveImage(id, side, blob);
      while (draftURLs.length) URL.revokeObjectURL(draftURLs.pop());
      if (existing) existing.fields = fields;
      else state.items.push({ id, fields });
      renderList();
      toast('Saved securely');
      if (isNew) {
        history.replaceState({ screen: 'detail', id }, '');
        openDetail(id, false);
      } else {
        history.back(); // back to the detail screen, re-rendered by popstate
      }
    } catch (e) {
      toast('Save failed: ' + e.message);
    } finally {
      btn.disabled = false;
    }
  }

  async function deleteCurrent() {
    const id = state.current;
    const item = state.items.find((i) => i.id === id);
    if (!item) return;
    const ok = await ask({ title: `Delete ${DocTypes.title(item.fields)}?`, text: 'This document and its photos will be permanently removed from this device.', ok: 'Delete', danger: true });
    if (!ok) return;
    await VaultDB.del('items', id);
    for (const side of ['front', 'back']) {
      await VaultDB.del('images', `${id}:${side}`);
      dropURL(`${id}:${side}`);
    }
    state.items = state.items.filter((i) => i.id !== id);
    renderList();
    goMain();
    toast('Document deleted');
  }

  function goMain() {
    state.current = null;
    renderList();
    show('main');
    history.replaceState({ screen: 'main' }, '');
  }

  // ---------- Settings: backup / passphrase ----------

  const boxOut = (b) => ({ iv: VaultCrypto.toB64(b.iv), data: VaultCrypto.toB64(b.data) });
  const boxIn = (b) => ({ iv: VaultCrypto.fromB64(b.iv), data: VaultCrypto.fromB64(b.data) });

  async function exportBackup() {
    const meta = await VaultDB.get('meta', 'vault');
    const items = await VaultDB.getAll('items');
    const images = await VaultDB.getAll('images');
    const backup = {
      format: 'id-vault',
      version: 1,
      salt: VaultCrypto.toB64(meta.salt),
      iterations: meta.iterations,
      check: boxOut(meta.check),
      items: items.map((r) => ({ id: r.id, box: boxOut(r.box) })),
      images: images.map((r) => ({ id: r.id, type: r.type, box: boxOut(r.box) })),
    };
    const blob = new Blob([JSON.stringify(backup)], { type: 'application/json' });
    const a = el('a', { href: URL.createObjectURL(blob), download: `id-vault-${new Date().toISOString().slice(0, 10)}.idvault` });
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    toast('Encrypted backup exported');
  }

  async function importBackup(file) {
    let backup;
    try {
      backup = JSON.parse(await file.text());
      if (backup.format !== 'id-vault') throw 0;
    } catch {
      return toast('That isn’t an ID Vault backup');
    }
    const res = await ask({ title: 'Import backup', text: 'Enter the passphrase that was used when this backup was made.', inputs: [{ type: 'password', placeholder: 'Backup passphrase' }], ok: 'Import' });
    if (!res || !res[0]) return;
    const key = await VaultCrypto.deriveKey(res[0], VaultCrypto.fromB64(backup.salt), backup.iterations);
    try {
      if ((await VaultCrypto.decryptJSON(key, boxIn(backup.check))) !== CHECK_VALUE) throw 0;
    } catch {
      return toast('Wrong passphrase for this backup');
    }
    // Re-encrypt everything with the current vault key so one passphrase opens it all.
    for (const r of backup.items) {
      const fields = await VaultCrypto.decryptJSON(key, boxIn(r.box));
      await VaultDB.put('items', { id: r.id, box: await VaultCrypto.encryptJSON(state.key, fields) });
    }
    for (const r of backup.images) {
      const bytes = await VaultCrypto.decryptBytes(key, boxIn(r.box));
      dropURL(r.id);
      await VaultDB.put('images', { id: r.id, type: r.type, box: await VaultCrypto.encryptBytes(state.key, bytes) });
    }
    await loadItems();
    renderList();
    toast(`Imported ${backup.items.length} document${backup.items.length === 1 ? '' : 's'}`);
  }

  async function changePassphrase() {
    const res = await ask({
      title: 'Change passphrase',
      text: 'Everything will be re-encrypted with the new passphrase.',
      inputs: [{ type: 'password', placeholder: 'New passphrase (8+ characters)' }, { type: 'password', placeholder: 'Repeat new passphrase' }],
      ok: 'Change',
    });
    if (!res) return;
    if (res[0].length < 8) return toast('Use at least 8 characters');
    if (res[0] !== res[1]) return toast('Passphrases don’t match');
    toast('Re-encrypting…');
    const salt = VaultCrypto.randomBytes(16);
    const key = await VaultCrypto.deriveKey(res[0], salt);
    const items = await VaultDB.getAll('items');
    const images = await VaultDB.getAll('images');
    const newItems = await Promise.all(items.map(async (r) => ({ id: r.id, box: await VaultCrypto.encryptJSON(key, await VaultCrypto.decryptJSON(state.key, r.box)) })));
    const newImages = await Promise.all(images.map(async (r) => ({ id: r.id, type: r.type, box: await VaultCrypto.encryptBytes(key, await VaultCrypto.decryptBytes(state.key, r.box)) })));
    for (const r of newItems) await VaultDB.put('items', r);
    for (const r of newImages) await VaultDB.put('images', r);
    await VaultDB.put('meta', { k: 'vault', salt, iterations: VaultCrypto.ITERATIONS, check: await VaultCrypto.encryptJSON(key, CHECK_VALUE) });
    state.key = key;
    toast('Passphrase changed');
  }

  async function wipe() {
    const res = await ask({
      title: 'Erase all data?',
      text: 'Every document and photo will be deleted from this device. Export a backup first if you want to keep them. Type ERASE to confirm.',
      inputs: [{ type: 'text', placeholder: 'ERASE', autocapitalize: 'characters' }],
      ok: 'Erase everything',
      danger: true,
    });
    if (!res || res[0].trim().toUpperCase() !== 'ERASE') return;
    await Promise.all(['meta', 'items', 'images'].map((s) => VaultDB.clear(s)));
    lock();
  }

  function openSettings(push = true) {
    $('#autolock').value = String(autoLockMinutes());
    show('settings');
    if (push) history.pushState({ screen: 'settings' }, '');
  }

  // ---------- Wiring ----------

  $('#lock-form').addEventListener('submit', onLockSubmit);
  for (const b of document.querySelectorAll('.pw-eye')) {
    b.onclick = () => {
      const inp = document.getElementById(b.dataset.target);
      const showing = inp.type === 'text';
      inp.type = showing ? 'password' : 'text';
      $('#pass2').type = inp.type;
      $('use', b).setAttribute('href', showing ? '#i-eye' : '#i-eye-off');
    };
  }
  $('#lock-now').onclick = lock;
  $('#add-btn').onclick = openPicker;
  $('#empty-add').onclick = openPicker;
  $('#open-settings').onclick = () => openSettings();
  $('#search').addEventListener('input', renderList);

  $('#d-edit').onclick = () => openEdit(state.current);
  $('#d-delete').onclick = deleteCurrent;
  $('#a-copy').onclick = () => {
    const item = state.items.find((i) => i.id === state.current);
    const { field, value } = primaryValue(item.fields);
    if (value) copy(value, field.label.replace(/\s*\(.*\)/, ''));
  };
  $('#a-reveal').onclick = () => { state.reveal = !state.reveal; openDetail(state.current, false); };
  $('#a-share').onclick = () => shareCurrent().catch((e) => toast(e.message));
  $('#a-photo').onclick = () => { const p = $('#d-photos .photo'); if (p) p.click(); };

  $('#e-save').onclick = saveEdit;
  $('#edit-form').addEventListener('submit', (e) => { e.preventDefault(); saveEdit(); });
  $('#edit-form').addEventListener('input', updatePreview);
  $('#edit-form').addEventListener('change', updatePreview);

  for (const b of document.querySelectorAll('.back')) b.onclick = () => history.back();

  $('#viewer').onclick = (e) => {
    if (e.target.closest('.close')) return closeViewer();
    if (e.target.tagName === 'IMG') $('#viewer').classList.toggle('zoom');
    else closeViewer();
  };

  $('#autolock').addEventListener('change', (e) => {
    try { localStorage.setItem('autolock', e.target.value); } catch { /* storage blocked */ }
    resetIdle();
    toast(`Auto-lock set to ${e.target.selectedOptions[0].textContent}`);
  });
  $('#settings').addEventListener('click', (e) => {
    const act = e.target.closest('[data-act]')?.dataset.act;
    const run = { export: exportBackup, import: () => $('#import-file').click(), 'change-pass': changePassphrase, wipe }[act];
    if (run) Promise.resolve(run()).catch((err) => toast(err.message || 'Something went wrong'));
  });
  $('#import-file').addEventListener('change', (e) => {
    const f = e.target.files[0];
    e.target.value = '';
    if (f) importBackup(f).catch((err) => toast('Import failed: ' + err.message));
  });

  window.addEventListener('popstate', (e) => {
    if (!state.key) return;
    closeViewer();
    $('#sheet').hidden = true;
    const s = e.state || {};
    if (s.screen === 'detail' && s.id) openDetail(s.id, false);
    else if (s.screen === 'picker') { renderPicker(); show('picker'); }
    else if (s.screen === 'edit') openEdit(s.id, s.type, 'none');
    else if (s.screen === 'settings') openSettings(false);
    else goMain();
  });

  setupPickers();
  initLock();

  if ('serviceWorker' in navigator && location.protocol !== 'file:') {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
  if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
})();
