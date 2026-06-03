interface AssetManifest {
  [key: string]: {
    file: string;
    name: string;
    src: string;
    isEntry: boolean;
  };
}

const importedMicroFrontend: Record<string, boolean> = {};

export function importMicroFrontend(url: string): void | Promise<void> {
  if (url in importedMicroFrontend) {
    return;
  }

  importedMicroFrontend[url] = true;

  const assetManifestUrl = `${url}/asset-manifest.json`;

  return fetch(assetManifestUrl)
    .then((res) => res.json())
    .then((manifest: AssetManifest) => importFirstEntry(url, manifest))
    .catch(() => {
      importedMicroFrontend[url] = false;

      // eslint-disable-next-line no-console
      console.error(`Failed to load micro frontend at url ${assetManifestUrl}`);
    });
}

async function importFirstEntry(url: string, manifest: AssetManifest) {
  const firstEntry = Object.values(manifest).find((entry) => entry.isEntry);
  if (firstEntry) {
    await import(/* @vite-ignore */ `${url}/${firstEntry.file}`);
  }
}
