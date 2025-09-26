export interface StoredFile {
  id?: number;
  name: string;
  data: Blob;
}

interface StoreFileOptions {
  clearStore: boolean;
}

export interface FileStorage {
  /** Store one or more files in object storage */
  store: (files: File | File[], opt?: StoreFileOptions) => Promise<void>;
  /** Get all stored files */
  getAll: () => Promise<StoredFile[]>;
  /** Clears object storage  */
  clear: () => Promise<void>;
}

export function useFileStorage(dbName: string = "arrfiles"): FileStorage {
  const STORE_NAME = "files";

  function createObjectStoreIfNotExists(db: IDBDatabase) {
    if (!db.objectStoreNames.contains(STORE_NAME)) {
      db.createObjectStore(STORE_NAME, { keyPath: "id", autoIncrement: true });
    }
  }

  async function openDB(): Promise<IDBDatabase> {
    return await new Promise((resolve, reject) => {
      const request = indexedDB.open(dbName);
      request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
        const db = (event.target as IDBOpenDBRequest).result;
        createObjectStoreIfNotExists(db);
      };
      request.onsuccess = function () {
        const db = this.result;
        resolve(db);
      };
      request.onerror = function () {
        reject(this.error);
      };
    });
  }

  async function store(
    files: File | File[],
    { clearStore }: StoreFileOptions = { clearStore: false },
  ): Promise<void> {
    const filesToStore = Array.isArray(files) ? files : [files];
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, "readwrite");
    const store = tx.objectStore(STORE_NAME);
    if (clearStore) {
      store.clear();
    }
    filesToStore.forEach((file) => {
      store.add({ name: file.name, data: file });
    });
    return new Promise((resolve, reject) => {
      tx.oncomplete = function () {
        db.close();
        resolve();
      };
      tx.onerror = function () {
        db.close();
        reject("Failed to add files");
      };
    });
  }

  async function getAll(): Promise<StoredFile[]> {
    const db = await openDB();
    if (!db.objectStoreNames.contains(STORE_NAME)) {
      db.close();
      return Promise.resolve([]);
    }
    const tx = db.transaction(STORE_NAME, "readonly");
    const store = tx.objectStore(STORE_NAME);
    const files: StoredFile[] = [];

    return new Promise((resolve, reject) => {
      const request = store.openCursor();
      request.onsuccess = (event: Event) => {
        const cursor = (event.target as IDBRequest<IDBCursorWithValue>).result;
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
        if (cursor) {
          files.push(cursor.value as StoredFile);
          cursor.continue();
        } else {
          db.close();
          resolve(files);
        }
      };
      request.onerror = () => reject(request.error);
    });
  }

  async function clear(): Promise<void> {
    return openDB().then((db) => {
      const tx = db.transaction(dbName, "readonly");
      const store = tx.objectStore(dbName);
      const req = store.clear();

      return new Promise((resolve, reject) => {
        req.onsuccess = function () {
          db.close();
          resolve();
        };
        req.onerror = function () {
          db.close();
          reject(`Failed to clear $name`);
        };
      });
    });
  }

  return { store, getAll, clear };
}
