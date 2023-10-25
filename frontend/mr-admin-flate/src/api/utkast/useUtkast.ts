import { useQuery } from "@tanstack/react-query";
import { UtkastDto } from "mulighetsrommet-api-client";
import { ZodSchema } from "zod";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export function useUtkast(
  schema: ZodSchema,
  utkastId?: string,
): { isLoading: boolean; data?: UtkastDto } {
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
      result.error.errors.forEach((error) => {
        delete utkastData[error.path[0]];
      });
      return { data: { ...query.data, utkastData }, isLoading };
    }
  }
  return { data: query.data, isLoading };
}
