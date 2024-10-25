import { Button, HGrid, SortState, Table } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";
import { PageHeader } from "~/components/PageHeader";
import { Deltaker, Refusjonskrav } from "~/domene/domene";
import { checkValidToken } from "~/auth/auth.server";
import { RefusjonKravDeltakelse } from "@mr/api-client";
import { useState } from "react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { formaterDato } from "~/utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { GenerelleDetaljer } from "~/components/refusjonskrav/GenerelleDetaljer";

export const meta: MetaFunction = () => {
  return [{ title: "Refusjon" }, { name: "description", content: "Refusjonsdetaljer" }];
};

type LoaderData = {
  krav: Refusjonskrav;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");

  const krav = await loadRefusjonskrav(params.id);

  return { krav };
};

interface ScopedSortState extends SortState {
  orderBy: RefusjonKravDeltakelse["navn"];
}

export default function RefusjonDeltakerlister() {
  const { krav } = useLoaderData<LoaderData>();
  const [sort, setSort] = useState<ScopedSortState | undefined>();

  const handleSort = (sortKey: ScopedSortState["orderBy"]) => {
    setSort(
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            orderBy: sortKey,
            direction:
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          },
    );
  };

  function comparator<T>(a: T, b: T, orderBy: keyof T): number {
    if (b[orderBy] == null || b[orderBy] < a[orderBy]) {
      return -1;
    }
    if (b[orderBy] > a[orderBy]) {
      return 1;
    }
    return 0;
  }

  const sortedData = krav.deltakere.slice().sort((a, b) => {
    if (sort) {
      return sort.direction === "ascending"
        ? comparator(b, a, sort.orderBy as keyof Deltaker)
        : comparator(a, b, sort.orderBy as keyof Deltaker);
    }
    return 1;
  });

  return (
    <>
      <PageHeader
        title="Deltakerliste"
        tilbakeLenke={{ navn: "Tilbake til refusjonsliste", url: "/" }}
      />
      <HGrid gap="5" columns={1}>
        <GenerelleDetaljer className="max-w-[50%]" krav={krav} />
        <Table
          sort={sort}
          onSortChange={(sortKey) => handleSort(sortKey as ScopedSortState["orderBy"])}
          zebraStripes
        >
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader scope="col" sortable sortKey="name">
                Navn
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col">Fødselsdato</Table.HeaderCell>
              <Table.HeaderCell scope="col">Startdato i tiltaket</Table.HeaderCell>
              <Table.ColumnHeader scope="col" sortable sortKey="startDatePeriod">
                Startdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey="endDatePeriod">
                Sluttdato i perioden
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col">Stillings-prosent</Table.HeaderCell>
              <Table.HeaderCell scope="col">Månedsverk i perioden</Table.HeaderCell>
              <Table.ColumnHeader scope="col" sortable sortKey="veileder">
                Veileder
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col"></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((deltaker) => {
              return (
                <Table.ExpandableRow key={deltaker.id} content={null} togglePlacement="right">
                  <Table.HeaderCell>{deltaker.navn}</Table.HeaderCell>
                  <Table.DataCell>{deltaker.norskIdent}</Table.DataCell>
                  <Table.DataCell>{deltaker.startDatoTiltaket}</Table.DataCell>
                  <Table.DataCell>
                    {deltaker.startDatoPerioden && formaterDato(deltaker.startDatoPerioden)}
                  </Table.DataCell>
                  <Table.DataCell>
                    {deltaker.sluttDatoPerioden && formaterDato(deltaker.sluttDatoPerioden)}
                  </Table.DataCell>
                  <Table.DataCell>{deltaker.stillingsprosent}</Table.DataCell>
                  <Table.DataCell>{deltaker.maanedsverk}</Table.DataCell>
                  <Table.DataCell>{deltaker.veileder}</Table.DataCell>
                </Table.ExpandableRow>
              );
            })}
          </Table.Body>
        </Table>
        <Definisjonsliste
          definitions={[
            {
              key: "Antall månedsverk",
              value: String(krav.beregning.antallManedsverk),
            },
            {
              key: "Beløp",
              value: formaterNOK(krav.beregning.belop),
            },
          ]}
        />
        <Button as={Link} className="justify-self-end" to={`/deltakerliste/detaljer/${krav.id}`}>
          Neste
        </Button>
      </HGrid>
    </>
  );
}
