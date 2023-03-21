import { Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { avtalePaginationAtom } from "../../../api/atoms";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { Avtalefilter } from "../../../components/filter/Avtalefilter";
import { PagineringContainer } from "../../../components/paginering/PagineringContainer";
import { PagineringsOversikt } from "../../../components/paginering/PagineringOversikt";
import { AVTALE_PAGE_SIZE } from "../../../constants";
import { AvtaleTabell } from "./AvtaleTabell";
import pageStyles from "../../Page.module.scss";

export function AvtalerForTiltakstype() {
  const [page, setPage] = useAtom(avtalePaginationAtom);
  const { data } = useAvtaler();

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
        <PagineringContainer>
          <PagineringsOversikt
            page={page}
            antall={avtaler.length}
            maksAntall={pagination.totalCount}
            type="avtaler"
          />
          <Pagination
            className={pageStyles.pagination}
            size="small"
            data-testid="paginering"
            page={page}
            onPageChange={setPage}
            count={Math.ceil(
              (pagination?.totalCount ?? AVTALE_PAGE_SIZE) / AVTALE_PAGE_SIZE
            )}
            data-version="v1"
          />
        </PagineringContainer>
      ) : null}
    </div>
  );
}
