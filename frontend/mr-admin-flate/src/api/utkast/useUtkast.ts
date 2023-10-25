import { useQuery } from "@tanstack/react-query";
import { ZodSchema } from "zod";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useUtkast<T extends object>(
  schema: ZodSchema<T>,
  utkastId?: string,
): { isLoading: boolean; data?: { id: string; utkastData: Partial<T> } } {
  const query = useQuery(
    QueryKeys.utkast(utkastId),
    () => mulighetsrommetClient.utkast.getUtkast({ id: utkastId! }),
    {
      enabled: !!utkastId,
    },
  );

  const isLoading = !!utkastId && query.isLoading;

  if (query.data) {
    const result = schema.safeParse(query.data.utkastData);
    const utkastData = query.data.utkastData;

    if (!result.success) {
      // Fjerner properties som ikke eksisterer lenger så ikke skjema krasjer
      result.error.errors.forEach((error) => {
        delete utkastData[error.path[0]];
      });
      return {
        data: { ...query.data, utkastData: utkastData as Partial<T> },
        isLoading,
      };
    }

    return {
      isLoading,
      data: { id: query.data.id, utkastData: query.data?.utkastData as Partial<T> },
    };
  }
  return {
    isLoading,
  };
}
