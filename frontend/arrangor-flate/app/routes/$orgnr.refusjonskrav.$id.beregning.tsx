import {
  ArrangorflateService,
  RefusjonKravDeltakelse,
  RefusjonKravDeltakelsePerson,
  RelevanteForslag,
} from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, Button, GuidePanel, HGrid, List, SortState, Table, VStack } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link, useLoaderData } from "react-router";
import { useState } from "react";
import { checkValidToken } from "~/auth/auth.server";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { PageHeader } from "~/components/PageHeader";
import { GenerelleDetaljer } from "~/components/refusjonskrav/GenerelleDetaljer";
import { Refusjonskrav } from "~/domene/domene";
import { loadRefusjonskrav } from "~/loaders/loadRefusjonskrav";
import { formaterDato, useOrgnrFromUrl } from "~/utils";
import { sortBy, SortBySelector, SortOrder } from "~/utils/sort-by";
import { LinkWithTabState } from "~/components/LinkWithTabState";
import { internalNavigation } from "~/internal-navigation";
import { hentMiljø, Miljø } from "~/services/miljø";

export const meta: MetaFunction = () => {
  return [{ title: "Refusjon" }, { name: "description", content: "Refusjonsdetaljer" }];
};

type LoaderData = {
  krav: Refusjonskrav;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
};
export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);
  const deltakerlisteUrl = deltakerOversiktLenke(hentMiljø());

  const { id } = params;
  if (!id) {
    throw Error("Mangler id");
  }

  const krav = await loadRefusjonskrav(id);
  const relevanteForslag = await ArrangorflateService.getRelevanteForslag({ id });

  return { krav, deltakerlisteUrl, relevanteForslag };
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
  const { krav, deltakerlisteUrl, relevanteForslag } = useLoaderData<LoaderData>();
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

  function hasRelevanteForslag(id: string): boolean {
    return (relevanteForslag.find((r) => r.deltakerId === id)?.antallRelevanteForslag ?? 0) > 0;
  }

  const deltakereMedRelevanteForslag = sortedData.filter((deltaker: RefusjonKravDeltakelse) =>
    hasRelevanteForslag(deltaker.id),
  );

  return (
    <>
      <PageHeader
        title="Beregning"
        tilbakeLenke={{
          navn: "Tilbake til refusjonskravliste",
          url: internalNavigation(orgnr).root,
        }}
      />
      <HGrid gap="5" columns={1}>
        <GenerelleDetaljer className="max-w-[50%]" krav={krav} />
        <GuidePanel>
          Hvis noen av opplysningene om deltakerne ikke stemmer, må det sendes forslag til Nav om
          endring via <Link to={deltakerlisteUrl}>Deltakeroversikten</Link>.
        </GuidePanel>
        {deltakereMedRelevanteForslag.length > 0 && (
          <Alert variant="warning">
            Det finnes ubehandlede forslag som påvirker refusjonen på følgende personer. Disse må
            først godkjennes av Nav veileder før refusjonskravet oppdaterer seg.
            <List>
              {deltakereMedRelevanteForslag.map((deltaker) => (
                <List.Item key={deltaker.id}>{deltaker.person.navn}</List.Item>
              ))}
            </List>
          </Alert>
        )}
        <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey)}>
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
            {sortedData.map((deltaker, index) => {
              const { id, person } = deltaker;
              const fodselsdato = getFormattedFodselsdato(person);
              return (
                <Table.ExpandableRow
                  key={id}
                  content={null}
                  togglePlacement="right"
                  className={
                    hasRelevanteForslag(id)
                      ? "bg-surface-warning-moderate"
                      : index % 2 !== 0
                        ? "bg-surface-subtle"
                        : "" // zebra stripes gjøres her fordi den overskriver warning background
                  }
                >
                  <Table.DataCell className="font-bold">{person?.navn}</Table.DataCell>
                  <Table.DataCell className="w-52">{fodselsdato}</Table.DataCell>
                  <Table.DataCell>{formaterDato(deltaker.startDato)}</Table.DataCell>
                  <Table.DataCell>{formaterDato(deltaker.forstePeriodeStartDato)}</Table.DataCell>
                  <Table.DataCell>{formaterDato(deltaker.sistePeriodeSluttDato)}</Table.DataCell>
                  <Table.DataCell>{deltaker.sistePeriodeDeltakelsesprosent}</Table.DataCell>
                  <Table.DataCell>{deltaker.manedsverk}</Table.DataCell>
                  <Table.DataCell>{deltaker.veileder}</Table.DataCell>
                </Table.ExpandableRow>
              );
            })}
          </Table.Body>
        </Table>
        <VStack align="end">
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
        </VStack>
        <Button
          as={LinkWithTabState}
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

function getDeltakerSelector(sortKey: DeltakerSortKey): SortBySelector<RefusjonKravDeltakelse> {
  switch (sortKey) {
    case DeltakerSortKey.PERSON_NAVN:
      return (d) => d.person?.navn;
    case DeltakerSortKey.PERIODE_START:
      return (d) => d.forstePeriodeStartDato;
    case DeltakerSortKey.PERIODE_SLUTT:
      return (d) => d.sistePeriodeSluttDato;
    case DeltakerSortKey.VEILEDER_NAVN:
      return (d) => d.veileder;
  }
}

function getFormattedFodselsdato(person?: RefusjonKravDeltakelsePerson) {
  return person?.fodselsdato
    ? formaterDato(person.fodselsdato)
    : person?.fodselsaar
      ? `Fødselsår: ${person.fodselsaar}`
      : null;
}

function deltakerOversiktLenke(miljo: Miljø): string {
  if (miljo === Miljø.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
