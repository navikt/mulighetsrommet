import { Heading, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { avtalePaginationAtom } from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { Avtalefilter } from "../../components/avtaler/Avtalefilter";
import { PagineringContainer } from "../../components/paginering/PagineringContainer";
import { PagineringsOversikt } from "../../components/paginering/PagineringOversikt";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { ListLayout } from "../../layouts/ListLayout";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";
import { AvtaleTabell } from "../tiltakstyper/avtaler/AvtaleTabell";

export function AvtalerPage() {
  const { data } = useAvtaler();
  const [page, setPage] = useAtom(avtalePaginationAtom);

  if (!data) {
    return null;
  }
  const pagination = data.pagination;
  const avtaler = data.data ?? [];

  return (
    <main>
      <NavigeringHeader />
      <ListLayout>
        <div className={styles.header_wrapper}>
          <Heading level="2" size="large">
            Oversikt over avtaler
          </Heading>
        </div>
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
              style={{ paddingBottom: "24px" }}
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
      </ListLayout>
    </main>
  );
}
