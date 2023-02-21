import { Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { avtalePaginationAtom } from "../../../api/atoms";
import { useAvtalerForTiltakstype } from "../../../api/avtaler/useAvtalerForTiltakstype";
import { Avtalefilter } from "../../../components/avtaler/Avtalefilter";
import { PagineringsOversikt } from "../../../components/paginering/PagineringOversikt";
import { AVTALE_PAGE_SIZE } from "../../../constants";
import styles from "./AvtalerForTiltakstype.module.scss";
import { AvtaleTabell } from "./AvtaleTabell";

export function AvtalerForTiltakstype() {
  const [page, setPage] = useAtom(avtalePaginationAtom);
  const { data } = useAvtalerForTiltakstype();

  if (!data) {
    return null;
  }

  const { data: avtaler = [], pagination } = data;

  return (
    <div>
      <Avtalefilter />
      <PagineringsOversikt
        page={page}
        antall={avtaler.length}
        maksAntall={pagination.totalCount}
        type="avtaler"
      />
      <AvtaleTabell />
      {avtaler.length > 0 ? (
        <div className={styles.paginering}>
          <PagineringsOversikt
            page={page}
            antall={avtaler.length}
            maksAntall={pagination.totalCount}
            type="avtaler"
          />
          <Pagination
            size="small"
            data-testid="paginering"
            page={page}
            onPageChange={setPage}
            count={Math.ceil(
              (pagination?.totalCount ?? AVTALE_PAGE_SIZE) / AVTALE_PAGE_SIZE
            )}
            data-version="v1"
          />
        </div>
      ) : null}
    </div>
  );
}
