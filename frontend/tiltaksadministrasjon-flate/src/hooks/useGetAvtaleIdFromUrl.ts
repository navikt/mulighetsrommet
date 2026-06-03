import { useRequiredParams } from "@/hooks/useRequiredParams";

export function useGetAvtaleIdFromUrlOrThrow(): string {
  const { avtaleId } = useRequiredParams(["avtaleId"]);
  return avtaleId;
}
