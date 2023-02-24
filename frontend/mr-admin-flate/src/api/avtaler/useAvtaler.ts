import { useQuery } from "@tanstack/react-query";
import { useAtom } from "jotai";
import { useDebounce } from "mulighetsrommet-frontend-common";
import { useParams } from "react-router-dom";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { avtaleFilter, avtalePaginationAtom } from "../atoms";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../QueryKeys";

export function useAvtaler() {
  const { tiltakstypeId } = useParams<{ tiltakstypeId: string | undefined }>();
  const [page] = useAtom(avtalePaginationAtom);
  const [filter] = useAtom(avtaleFilter);
  const debouncedSok = useDebounce(filter.sok, 300);

  return useQuery(
    QueryKeys.avtaler(
      tiltakstypeId || "",
      debouncedSok,
      filter.status,
      filter.enhet,
      filter.sortering,
      page
    ),
    () => {
      return mulighetsrommetClient.avtaler.getAvtaler({
        tiltakstypeId,
        search: debouncedSok || undefined,
        avtalestatus: filter.status ? filter.status : undefined,
        enhet: filter.enhet ? filter.enhet : undefined,
        sort: filter.sortering,
        page,
        size: AVTALE_PAGE_SIZE,
      });
    }
  );
}
