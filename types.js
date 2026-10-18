// Document catalogue. Each type defines its card look and the fields it asks for.
// Field kinds: text, date, cpf (auto-formatted), select, textarea.
// `primary` marks the field shown as the big number on the card.
// `secret` fields are masked until revealed.
const DocTypes = (() => {
  const UFS = 'AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO'.split(' ');

  const holder = { k: 'holder', label: 'Full name' };
  const cpf = { k: 'cpf', label: 'CPF', kind: 'cpf', secret: true, mono: true };
  const birth = { k: 'birth', label: 'Date of birth', kind: 'date' };
  const issued = { k: 'issued', label: 'Issue date', kind: 'date' };
  const expires = { k: 'expires', label: 'Valid until', kind: 'date' };
  const uf = { k: 'uf', label: 'State (UF)', kind: 'select', opts: ['', ...UFS] };

  const list = [
    {
      key: 'rg', abbr: 'RG', name: 'Registro Geral', sub: 'Carteira de Identidade',
      c1: '#0f5c4a', c2: '#2a9d7c', ink: '#eafff7',
      fields: [
        holder,
        { k: 'number', label: 'RG number', primary: true, secret: true, mono: true },
        { k: 'issuer', label: 'Issuing body', placeholder: 'SSP' },
        uf, cpf, birth, issued,
        { k: 'birthplace', label: 'Place of birth' },
        { k: 'parents', label: 'Parents (filiação)', kind: 'textarea' },
      ],
    },
    {
      key: 'cin', abbr: 'CIN', name: 'Carteira de Identidade Nacional', sub: 'Nova identidade',
      c1: '#0b3d91', c2: '#1d74d8', ink: '#eef5ff',
      fields: [holder, { ...cpf, primary: true }, birth, issued, expires, { k: 'birthplace', label: 'Place of birth' }],
    },
    {
      key: 'cnh', abbr: 'CNH', name: 'Carteira Nacional de Habilitação', sub: 'Driver’s license',
      c1: '#1d5a2c', c2: '#7fb238', ink: '#f4ffe9',
      fields: [
        holder,
        { k: 'number', label: 'Registration no. (Nº registro)', primary: true, secret: true, mono: true },
        { k: 'category', label: 'Category', kind: 'select', opts: ['', 'A', 'B', 'AB', 'C', 'D', 'E', 'AC', 'AD', 'AE'] },
        cpf, expires,
        { k: 'firstLicense', label: 'First license (1ª habilitação)', kind: 'date' },
        issued,
        { k: 'issuer', label: 'Issuer', placeholder: 'DETRAN-SP' },
      ],
    },
    {
      key: 'cpf', abbr: 'CPF', name: 'Cadastro de Pessoas Físicas', sub: 'Receita Federal',
      c1: '#14284b', c2: '#2f5597', ink: '#eef3ff',
      fields: [holder, { ...cpf, k: 'number', primary: true }, birth],
    },
    {
      key: 'creci', abbr: 'CRECI', name: 'Conselho Regional de Corretores de Imóveis', sub: 'Real estate broker',
      c1: '#151a2e', c2: '#3a3f63', ink: '#f6ecd2', gold: true,
      fields: [
        holder,
        { k: 'number', label: 'Registration no. (Inscrição)', primary: true, mono: true },
        { k: 'region', label: 'Region', placeholder: 'CRECI-SP · 2ª Região' },
        { k: 'kind', label: 'Registration type', kind: 'select', opts: ['', 'Pessoa Física', 'Pessoa Jurídica', 'Estagiário'] },
        cpf, issued, expires,
      ],
    },
    {
      key: 'council', abbr: 'CONSELHO', name: 'Conselho profissional', sub: 'OAB · CRM · CREA · CRC…',
      c1: '#3b1d4f', c2: '#7a3e9d', ink: '#f8eeff',
      fields: [
        holder,
        { k: 'council', label: 'Council', kind: 'select', opts: ['', 'OAB', 'CRM', 'CREA', 'CRC', 'CRO', 'CRF', 'COREN', 'CRP', 'CAU', 'CREFITO', 'CRN', 'Outro'] },
        { k: 'number', label: 'Registration number', primary: true, mono: true },
        uf, cpf, issued, expires,
      ],
    },
    {
      key: 'titulo', abbr: 'TÍTULO', name: 'Título de Eleitor', sub: 'Justiça Eleitoral',
      c1: '#5a1a1a', c2: '#a8432f', ink: '#fff1ec',
      fields: [
        holder,
        { k: 'number', label: 'Voter registration no.', primary: true, mono: true },
        { k: 'zone', label: 'Zone (zona)' },
        { k: 'section', label: 'Section (seção)' },
        { k: 'city', label: 'City' }, uf, issued,
      ],
    },
    {
      key: 'passport', abbr: 'PASSAPORTE', name: 'República Federativa do Brasil', sub: 'Passport',
      c1: '#0e1f3d', c2: '#1f3d73', ink: '#f3efe0', gold: true,
      fields: [
        holder,
        { k: 'number', label: 'Passport number', primary: true, secret: true, mono: true },
        { k: 'country', label: 'Issuing country', placeholder: 'Brasil' },
        { k: 'nationality', label: 'Nationality' },
        birth, issued, expires,
      ],
    },
    {
      key: 'ctps', abbr: 'CTPS', name: 'Carteira de Trabalho', sub: 'Digital ou física',
      c1: '#173b5c', c2: '#2d6e9e', ink: '#eef7ff',
      fields: [holder, { k: 'number', label: 'CTPS number', primary: true, mono: true }, { k: 'series', label: 'Series (série)' }, uf, { k: 'pis', label: 'PIS / NIS', mono: true }, cpf],
    },
    {
      key: 'pis', abbr: 'PIS/NIS', name: 'PIS · PASEP · NIS', sub: 'Número social',
      c1: '#164a43', c2: '#2f8077', ink: '#ecfffb',
      fields: [holder, { k: 'number', label: 'Number', primary: true, mono: true }, cpf],
    },
    {
      key: 'sus', abbr: 'SUS', name: 'Cartão Nacional de Saúde', sub: 'CNS',
      c1: '#08506b', c2: '#1a97b8', ink: '#eafaff',
      fields: [holder, { k: 'number', label: 'CNS number', primary: true, mono: true }, cpf, birth],
    },
    {
      key: 'health', abbr: 'SAÚDE', name: 'Plano de saúde', sub: 'Health insurance',
      c1: '#7a1f4a', c2: '#c74a7d', ink: '#fff0f6',
      fields: [
        holder,
        { k: 'provider', label: 'Provider', placeholder: 'Unimed, Amil, SulAmérica…' },
        { k: 'number', label: 'Member number (carteirinha)', primary: true, mono: true },
        { k: 'plan', label: 'Plan' }, expires,
      ],
    },
    {
      key: 'crlv', abbr: 'CRLV', name: 'Documento do veículo', sub: 'CRLV-e',
      c1: '#3d3d3d', c2: '#6f6f6f', ink: '#ffffff',
      fields: [
        { k: 'holder', label: 'Owner' },
        { k: 'plate', label: 'Plate (placa)', primary: true, mono: true },
        { k: 'renavam', label: 'RENAVAM', mono: true },
        { k: 'model', label: 'Make / model' },
        { k: 'year', label: 'Year' }, expires,
      ],
    },
    {
      key: 'reservista', abbr: 'RESERVISTA', name: 'Certificado de Reservista', sub: 'Serviço militar',
      c1: '#3c4a24', c2: '#6b7d3a', ink: '#f6fbe9',
      fields: [holder, { k: 'number', label: 'Certificate number', primary: true, mono: true }, { k: 'ra', label: 'RA' }, { k: 'series', label: 'Series' }, cpf, issued],
    },
    {
      key: 'certidao', abbr: 'CERTIDÃO', name: 'Certidão de registro civil', sub: 'Nascimento · Casamento',
      c1: '#5b4a2e', c2: '#9c7f4b', ink: '#fff8ea',
      fields: [
        holder,
        { k: 'kind', label: 'Type', kind: 'select', opts: ['', 'Nascimento', 'Casamento', 'Óbito'] },
        { k: 'number', label: 'Registration (matrícula)', primary: true, mono: true },
        { k: 'registry', label: 'Registry office (cartório)' }, issued,
      ],
    },
    {
      key: 'other', abbr: 'ID', name: 'Other document', sub: 'Anything else',
      c1: '#1f2937', c2: '#4b5563', ink: '#f3f4f6',
      fields: [
        { k: 'title', label: 'Document name', placeholder: 'e.g. Student ID', required: true },
        holder,
        { k: 'number', label: 'Number', primary: true, mono: true },
        { k: 'issuer', label: 'Issuer' }, issued, expires,
      ],
    },
  ];

  const byKey = Object.fromEntries(list.map((t) => [t.key, t]));

  // Map items saved by the first version (free-text type names) onto the catalogue.
  const LEGACY = { "passport": 'passport', "driver's license": 'cnh', "national id": 'rg', "health insurance": 'health' };

  function get(key) {
    return byKey[key] || byKey.other;
  }

  function normalize(fields) {
    if (byKey[fields.type]) return fields;
    const mapped = LEGACY[(fields.type || '').toLowerCase()] || 'other';
    return { ...fields, type: mapped, title: fields.title || fields.name || fields.type || '' };
  }

  function title(fields) {
    const t = get(fields.type);
    if (t.key === 'other') return fields.title || 'Document';
    if (t.key === 'council' && fields.council) return fields.council;
    return t.abbr;
  }

  function formatCPF(v) {
    const d = String(v).replace(/\D/g, '').slice(0, 11);
    return d
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
  }

  return { list, get, normalize, title, formatCPF };
})();
