// Minimal IndexedDB wrapper. Stores:
//   meta:   { k: 'vault', salt, iterations, check }   (check = encrypted known value)
//   items:  { id, box }        box = encrypted JSON with fields
//   images: { id, box }        box = encrypted image bytes; id = `${itemId}:${side}`
const VaultDB = (() => {
  let dbp;

  function open() {
    if (dbp) return dbp;
    dbp = new Promise((resolve, reject) => {
      const req = indexedDB.open('id-vault', 1);
      req.onupgradeneeded = () => {
        const db = req.result;
        db.createObjectStore('meta', { keyPath: 'k' });
        db.createObjectStore('items', { keyPath: 'id' });
        db.createObjectStore('images', { keyPath: 'id' });
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    return dbp;
  }

  async function run(store, mode, fn) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(store, mode);
      const result = fn(tx.objectStore(store));
      tx.oncomplete = () => resolve(result && 'result' in result ? result.result : undefined);
      tx.onerror = () => reject(tx.error);
      tx.onabort = () => reject(tx.error);
    });
  }

  const get = (store, key) => run(store, 'readonly', (s) => s.get(key));
  const getAll = (store) => run(store, 'readonly', (s) => s.getAll());
  const put = (store, value) => run(store, 'readwrite', (s) => s.put(value));
  const del = (store, key) => run(store, 'readwrite', (s) => s.delete(key));
  const clear = (store) => run(store, 'readwrite', (s) => s.clear());

  return { get, getAll, put, del, clear };
})();
