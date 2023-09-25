import { useParams } from "react-router-dom";

export function useGetAvtaleIdFromUrl() {
  const { avtaleId } = useParams<{
    avtaleId: string;
  }>();
  return avtaleId;
}
