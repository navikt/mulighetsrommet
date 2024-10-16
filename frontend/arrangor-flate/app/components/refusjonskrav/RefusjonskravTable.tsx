import { RefusjonKravAft, RefusjonskravStatus } from "@mr/api-client";
import { Tag } from "@navikt/ds-react";
import { Link } from "@remix-run/react";
import { ReactNode } from "react";
import { formaterDato } from "~/utils";
import { formaterTall } from "@mr/frontend-common/utils/utils";

interface Props {
  krav: RefusjonKravAft[];
}

const TableTitle = ({ children, className }: { children: ReactNode; className?: string }) => {
  return <div className={"font-bold " + className}>{children}</div>;
};

export function RefusjonskravTable({ krav }: Props) {
  return (
    <>
      <div className="border-spacing-y-6 border-collapsed mt-4">
        <div>
          <div className="grid grid-cols-12 w-full gap-4">
            <TableTitle className={"col-span-3"}>Periode</TableTitle>
            <TableTitle className={"col-span-2"}>Månedsverk</TableTitle>
            <TableTitle className={"col-span-2"}>Beløp</TableTitle>
            <TableTitle className={"col-span-2"}>Innsendingsfrist</TableTitle>
            <TableTitle>Status</TableTitle>
            <div></div>
          </div>
        </div>
        <div>
          {krav.map(({ id, status, beregning, gjennomforing }) => {
            return (
              <div
                className={
                  getRowStyle(status) + "grid grid-cols-12 gap-4 border border-[#122B4414] my-4"
                }
                key={id}
              >
                <div className={"col-span-12 bg-[#122B4414] p-1"}>{gjennomforing.navn}</div>
                <div className={"grid grid-cols-12 col-span-12 p-2"}>
                  <div className={"col-span-3"}>
                    {`${formaterDato(beregning.periodeStart)} - ${formaterDato(beregning.periodeSlutt)}`}
                  </div>
                  <div className="col-span-2">{beregning.antallManedsverk}</div>
                  <div className="col-span-2">{formaterTall(beregning.belop)} NOK</div>
                  <div className="col-span-2">Frist for godkjenning</div>
                  <div className="col-span-2">{statusTilTag(status)}</div>
                  <div className="col-span-1">
                    <Link
                      className="hover:underline font-bold no-underline"
                      to={`for-du-begynner/${id}`}
                    >
                      Detaljer
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </>
  );
}

function getRowStyle(status: RefusjonskravStatus) {
  return status === RefusjonskravStatus.NARMER_SEG_FRIST ? "bg-surface-warning-moderate" : "";
}

function statusTilTag(status: RefusjonskravStatus): ReactNode {
  switch (status) {
    case RefusjonskravStatus.GODKJENT_AV_ARRANGOR:
      return <Tag variant="neutral">Godkjent</Tag>;
    case RefusjonskravStatus.KLAR_FOR_GODKJENNING:
      return <Tag variant="alt1">Klar for innsending</Tag>;
    case RefusjonskravStatus.NARMER_SEG_FRIST:
      return <Tag variant="warning">Nærmer seg frist</Tag>;
  }
}
