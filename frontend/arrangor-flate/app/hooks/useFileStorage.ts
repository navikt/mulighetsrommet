export interface StoredFile {
  id?: number;
  name: string;
  data: Blob;
}

interface StoreFileOptions {
  clearStore: boolean;
}

export interface FileStorage {
  store: (files: File | File[], opt: StoreFileOptions) => Promise<void>;
  getAll: () => Promise<StoredFile[]>;
  deleteDatabase: () => Promise<void>;
}

export function useFileStorage(storeName: string = "arrfiles"): FileStorage {
  function createObjectStoreIfNotExists(db: IDBDatabase) {
    if (!db.objectStoreNames.contains(storeName)) {
      db.createObjectStore(storeName, { keyPath: "id", autoIncrement: true });
    }
  }

  async function openDB(): Promise<IDBDatabase> {
    return await new Promise((resolve, reject) => {
      const request = indexedDB.open(storeName);
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
    const tx = db.transaction(storeName, "readwrite");
    const store = tx.objectStore(storeName);
    if (clearStore) {
      store.clear();
    }
    filesToStore.forEach((file) => {
      store.add({ name: file.name, data: file });
    });
    return Promise.resolve();
  }

  async function getAll(): Promise<StoredFile[]> {
    const db = await openDB();
    const tx = db.transaction(storeName, "readonly");
    const store = tx.objectStore(storeName);
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

  function deleteDatabase(): Promise<void> {
    return new Promise((resolve, reject) => {
      const deleteRequest = window.indexedDB.deleteDatabase(storeName);

      deleteRequest.onsuccess = () => resolve();
      deleteRequest.onerror = () => reject(deleteRequest.error);
    });
  }

  return { store, getAll, deleteDatabase };
}
