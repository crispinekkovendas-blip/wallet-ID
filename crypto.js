// Passphrase-based encryption: PBKDF2-SHA256 -> AES-256-GCM.
// The key only ever lives in memory while the vault is unlocked.
const VaultCrypto = (() => {
  const ITERATIONS = 600000;
  const enc = new TextEncoder();
  const dec = new TextDecoder();

  const randomBytes = (n) => crypto.getRandomValues(new Uint8Array(n));

  async function deriveKey(passphrase, salt, iterations = ITERATIONS) {
    const base = await crypto.subtle.importKey('raw', enc.encode(passphrase), 'PBKDF2', false, ['deriveKey']);
    return crypto.subtle.deriveKey(
      { name: 'PBKDF2', salt, iterations, hash: 'SHA-256' },
      base,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  // Returns { iv, data } with data as ArrayBuffer.
  async function encryptBytes(key, bytes) {
    const iv = randomBytes(12);
    const data = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, bytes);
    return { iv, data };
  }

  async function decryptBytes(key, box) {
    return crypto.subtle.decrypt({ name: 'AES-GCM', iv: box.iv }, key, box.data);
  }

  const encryptJSON = (key, obj) => encryptBytes(key, enc.encode(JSON.stringify(obj)));
  const decryptJSON = async (key, box) => JSON.parse(dec.decode(await decryptBytes(key, box)));

  function toB64(buf) {
    const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
    let s = '';
    for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
    return btoa(s);
  }

  function fromB64(s) {
    const bin = atob(s);
    const out = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
    return out;
  }

  return { ITERATIONS, randomBytes, deriveKey, encryptBytes, decryptBytes, encryptJSON, decryptJSON, toB64, fromB64 };
})();
