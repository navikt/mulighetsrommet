import { useParams } from "react-router-dom";

export function useGetAvtaleIdFromUrl() {
  const { avtaleId } = useParams<{
    avtaleId: string;
  }>();
  return avtaleId;
}

export function useGetAvtaleIdFromUrlOrThrow(): string {
  const { avtaleId } = useParams<{ avtaleId: string }>();

  if (!avtaleId) {
    throw Error("AvtaleId er ikke satt i URL");
  }

  return avtaleId;
}
