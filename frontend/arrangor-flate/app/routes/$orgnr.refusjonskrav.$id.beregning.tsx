import { Button, HGrid, SortState, Table } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";
import { PageHeader } from "~/components/PageHeader";
import { Deltaker, Refusjonskrav } from "~/domene/domene";
import { checkValidToken } from "~/auth/auth.server";
import { useState } from "react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { GenerelleDetaljer } from "~/components/refusjonskrav/GenerelleDetaljer";
import { sortBy, SortBySelector, SortOrder } from "~/utils/sort-by";
import { RefusjonKravDeltakelsePerson } from "@mr/api-client";
import { internalNavigation } from "../internal-navigation";

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

interface DeltakerSortState extends SortState {
  direction: SortOrder;
  orderBy: DeltakerSortKey;
}

enum DeltakerSortKey {
  PERSON_NAVN = "PERSON_NAVN",
  PERIODE_START = "PERIODE_START",
  PERIODE_SLUTT = "PERIODE_SLUTT",
  VEILEDER_NAVN = "VEILEDER_NAVN",
}

export default function RefusjonskravBeregning() {
  const orgnr = useOrgnrFromUrl();
  const { krav } = useLoaderData<LoaderData>();
  const [sort, setSort] = useState<DeltakerSortState | undefined>();

  const handleSort = (orderBy: string) => {
    if (!isDeltakerSortKey(orderBy)) {
      return;
    }

    if (sort && orderBy === sort.orderBy && sort.direction === "descending") {
      setSort(undefined);
    } else {
      const direction =
        sort && orderBy === sort.orderBy && sort.direction === "ascending"
          ? "descending"
          : "ascending";
      setSort({ orderBy, direction });
    }
  };

  const sortedData = sort
    ? sortBy(krav.deltakere, sort.direction, getDeltakerSelector(sort.orderBy))
    : krav.deltakere;

  return (
    <>
      <PageHeader
        title="Beregning"
        tilbakeLenke={{ navn: "Tilbake til refusjonskravliste", url: "/" }}
      />
      <HGrid gap="5" columns={1}>
        <GenerelleDetaljer className="max-w-[50%]" krav={krav} />
        <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey)} zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERSON_NAVN}>
                Navn
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Fødselsdato</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Startdato i tiltaket</Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERIODE_START}>
                Startdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.PERIODE_SLUTT}>
                Sluttdato i perioden
              </Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Deltakelsesprosent</Table.ColumnHeader>
              <Table.ColumnHeader scope="col">Månedsverk i perioden</Table.ColumnHeader>
              <Table.ColumnHeader scope="col" sortable sortKey={DeltakerSortKey.VEILEDER_NAVN}>
                Veileder
              </Table.ColumnHeader>
              <Table.HeaderCell scope="col"></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sortedData.map((deltaker) => {
              const { id, person, startDatoPerioden, sluttDatoPerioden } = deltaker;
              const fodselsdato = getFormattedFodselsdato(person);
              return (
                <Table.ExpandableRow key={id} content={null} togglePlacement="right">
                  <Table.DataCell className="font-bold">{person.navn}</Table.DataCell>
                  <Table.DataCell className="w-52">{fodselsdato}</Table.DataCell>
                  <Table.DataCell>
                    {deltaker.startDatoTiltaket && formaterDato(deltaker.startDatoTiltaket)}
                  </Table.DataCell>
                  <Table.DataCell>
                    {startDatoPerioden && formaterDato(startDatoPerioden)}
                  </Table.DataCell>
                  <Table.DataCell>
                    {sluttDatoPerioden && formaterDato(sluttDatoPerioden)}
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
        <Button
          as={Link}
          className="justify-self-end"
          to={internalNavigation(orgnr).bekreft(krav.id)}
        >
          Neste
        </Button>
      </HGrid>
    </>
  );
}

function isDeltakerSortKey(sortKey: string): sortKey is DeltakerSortKey {
  return sortKey in DeltakerSortKey;
}

function getDeltakerSelector(sortKey: DeltakerSortKey): SortBySelector<Deltaker> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.person?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.startDatoPerioden;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.sluttDatoPerioden;
    case DeltakerSortKey.VEILEDER_NAVN:
      return (d) => d.veileder;
  }
}

function getFormattedFodselsdato(person: RefusjonKravDeltakelsePerson) {
  return person.foedselsdato
    ? formaterDato(person.foedselsdato)
    : person.fodselsaar
      ? `Fødselsår: ${person.fodselsaar}`
      : null;
}
