import { useQuery } from "@tanstack/react-query";

export function useManifest(manifestUrl: string) {
  return useQuery<any>({
    queryKey: ["manifest"],

    ...async () => {
      const response = await fetch(manifestUrl);
      if (!response.ok) {
        throw new Error("Could not fetch");
      }

      return await response.json();
    },
  });
}
