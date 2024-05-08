import { useQuery } from "@tanstack/react-query";

export interface NusDataFraSsb {
  classificationItems: {
    code: string;
    parentCode: string;
    level: string;
    name: string;
  }[];
}

export function useNusData() {
  return useQuery<NusDataFraSsb>({
    queryKey: ["nusdata"],
    queryFn: async () => {
      const version = "2437";
      const response = await fetch(`https://data.ssb.no/api/klass/v1/versions/${version}`);
      if (response.ok) {
        return await response.json();
      }
      throw new Error();
    },
  });
}
