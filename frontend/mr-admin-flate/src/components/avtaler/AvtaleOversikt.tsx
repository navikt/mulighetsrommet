import { Alert, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { avtalePaginationAtom } from "../../api/atoms";
import { Laster } from "../laster/Laster";
import styles from "../listeelementer/Listeelementer.module.scss";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { AvtaleTabell } from "../../pages/tiltakstyper/avtaler/AvtaleTabell";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { AVTALE_PAGE_SIZE } from "../../constants";

export function AvtaleOversikt() {
  const { data, isLoading, isError } = useAvtaler();
  const [page, setPage] = useAtom(avtalePaginationAtom);

  const pagination = data?.pagination;
  const avtaler = data?.data ?? [];

  if (!data && isLoading) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  if (!data) {
    return <Alert variant="info">Fant ingen avtaler</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">Vi hadde problemer med henting av avtaler</Alert>
    );
  }

  return (
    <>
      <PagineringsOversikt
        page={page}
        antall={avtaler.length}
        maksAntall={pagination?.totalCount}
        type="avtaler"
      />
      <AvtaleTabell />
      {avtaler.length > 0 ? (
        <PagineringContainer>
          <PagineringsOversikt
            page={page}
            antall={avtaler.length}
            maksAntall={pagination?.totalCount}
            type="avtaler"
          />
          <Pagination
            className={styles.pagination}
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
    </>
  );
}
