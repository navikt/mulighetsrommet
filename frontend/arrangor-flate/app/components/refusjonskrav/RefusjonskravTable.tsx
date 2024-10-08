import { RefusjonskravDto, RefusjonskravStatus } from "@mr/api-client";
import { Table, Tag } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import { ReactNode } from "react";

interface Props {
  krav: RefusjonskravDto[];
}

const TableTitle = () => {};

export function RefusjonskravTable({ krav }: Props) {
  return (
    <>
      <div className="border-spacing-y-6 border-collapsed">
        <div>
          <div className="grid grid-cols-6 w-full gap-4">
            <div>Periode</div>
            <div>Månedsverk</div>
            <div>Beløp</div>
            <div>Frist for godkjenning</div>
            <div>Status</div>
            <div></div>
          </div>
        </div>
        <Table.Body>
          {krav.map(({ id, periodeStart, periodeSlutt, beregning, tiltakstype }) => {
            // TODO: Hardkodet enn så lenge
            const status = RefusjonskravStatus.KLAR_FOR_INNSENDING;

            return (
              <div className={getRowStyle(status) + "grid grid-cols-6 gap-4"} key={id}>
                <div>{`${periodeStart} - ${periodeSlutt}`}</div>
                <div>123</div>
                <div>{beregning.belop} NOK</div>
                <div>Frist for godkjenning</div>
                <div>{statusTilTag(status)}</div>
                <div>
                  <Link
                    className="hover:underline font-bold no-underline"
                    to={`for-du-begynner/${id}`}
                  >
                    Detaljer
                  </Link>
                </div>
              </div>
            );
          })}
        </Table.Body>
      </div>
    </>
  );
}

function getRowStyle(status: RefusjonskravStatus) {
  return status === RefusjonskravStatus.NARMER_SEG_FRIST ? "bg-surface-warning-moderate" : "";
}

function statusTilTag(status: RefusjonskravStatus): ReactNode {
  switch (status) {
    case RefusjonskravStatus.ATTESTERT:
      return <Tag variant="neutral">Attestert</Tag>;
    case RefusjonskravStatus.KLAR_FOR_INNSENDING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
