import { Accordion, Search, Skeleton, VStack } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { AvtaleFilter as AvtaleFilterProps, avtaleFilterAccordionAtom } from "../../api/atoms";
import { CheckboxList } from "./Tiltaksgjennomforingfilter";
import {
  AVTALE_STATUS_OPTIONS,
  regionOptions,
  tiltakstypeOptions,
  virksomhetOptions,
} from "../../utils/filterUtils";

type Filters = "tiltakstype";

interface Props {
  filterAtom: WritableAtom<AvtaleFilterProps, [newValue: AvtaleFilterProps], void>;
  skjulFilter?: Record<Filters, boolean>;
}

export function AvtaleFilter({ filterAtom, skjulFilter }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(avtaleFilterAccordionAtom);
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const { data: virksomheter, isLoading: isLoadingVirksomheter } = useVirksomheter(
    VirksomhetTil.AVTALE,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1,
  );
  if (
    !enheter ||
    isLoadingEnheter ||
    !virksomheter ||
    isLoadingVirksomheter ||
    !tiltakstyper ||
    isLoadingTiltakstyper
  ) {
    return (
      <VStack gap="2">
        <Skeleton height={50} variant="rounded" />
        <Skeleton height={200} variant="rounded" />
        <Skeleton height={50} variant="rounded" />
        <Skeleton height={50} variant="rounded" />
        <Skeleton height={50} variant="rounded" />
      </VStack>
    );
  }

  return (
    <div>
      <Search
        label="Søk etter tiltaksgjennomføring"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn eller tiltaksnr."
        onChange={(search: string) => {
          setFilter({
            ...filter,
            page: 1,
            sok: search,
          });
        }}
        value={filter.sok}
        aria-label="Søk etter tiltaksgjennomføring"
      />
      <Accordion>
        <Accordion.Item open={accordionsOpen.includes("status")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "status")]);
            }}
          >
            Status
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={AVTALE_STATUS_OPTIONS}
              isChecked={(status) => filter.statuser.includes(status)}
              onChange={(status) => {
                setFilter({
                  ...filter,
                  page: 1,
                  statuser: addOrRemove(filter.statuser, status),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        {!skjulFilter?.tiltakstype && (
          <Accordion.Item open={accordionsOpen.includes("tiltakstype")}>
            <Accordion.Header
              onClick={() => {
                setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstype")]);
              }}
            >
              Tiltakstype
            </Accordion.Header>
            <Accordion.Content>
              <CheckboxList
                items={tiltakstypeOptions(tiltakstyper.data)}
                isChecked={(tiltakstype) => filter.tiltakstyper.includes(tiltakstype)}
                onChange={(tiltakstype) => {
                  setFilter({
                    ...filter,
                    page: 1,
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                  });
                }}
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
        <Accordion.Item open={accordionsOpen.includes("region")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "region")]);
            }}
          >
            Region
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={regionOptions(enheter)}
              isChecked={(region) => filter.navRegioner.includes(region)}
              onChange={(region) => {
                setFilter({
                  ...filter,
                  page: 1,
                  navRegioner: addOrRemove(filter.navRegioner, region),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("leverandor")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "leverandor")]);
            }}
          >
            Leverandør
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              searchable
              items={virksomhetOptions(virksomheter)}
              isChecked={(orgnr) => filter.leverandor.includes(orgnr)}
              onChange={(orgnr) => {
                setFilter({
                  ...filter,
                  page: 1,
                  leverandor: addOrRemove(filter.leverandor, orgnr),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
