import { useState } from "react";

type DownloadHandlerResult = { data: Blob | File; response: Response };

export function useDownloadFile(
  handler: () => Promise<DownloadHandlerResult>,
): [boolean, () => void] {
  const [downloading, setDownloading] = useState(false);

  const download = async () => {
    setDownloading(true);
    try {
      const { data, response } = await handler();
      downloadFile(data, response);
    } finally {
      setDownloading(false);
    }
  };

  return [downloading, download];
}

export function downloadFile(data: Blob | File, response: Response) {
  const url = URL.createObjectURL(data);
  const a = document.createElement("a");
  a.href = url;
  a.download = resolveFileName(response);
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export function resolveFileName(response: Response) {
  const contentDisposition = response.headers.get("Content-Disposition");
  const fileNameMatch = contentDisposition?.match(/filename="?([^"]+)"?/);
  const fileName = fileNameMatch?.[1];
  if (!fileName) {
    throw new Error("Failed to resolve filename from Content-Disposition");
  }
  return fileName;
}
