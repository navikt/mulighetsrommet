import { Accordion, Checkbox, Search, Skeleton, VStack } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import {
  TiltaksgjennomforingFilter as TiltaksgjennomforingFilterProps,
  gjennomforingFilterAccordionAtom,
} from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import styles from "./Filter.module.scss";
import {
  enhetOptions,
  regionOptions,
  TILTAKSGJENNOMFORING_STATUS_OPTIONS,
  tiltakstypeOptions,
  virksomhetOptions,
} from "../../utils/filterUtils";

type Filters = "tiltakstype";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilterProps,
    [newValue: TiltaksgjennomforingFilterProps],
    void
  >;
  skjulFilter?: Record<Filters, boolean>;
}

export function TiltaksgjennomforingFilter({ filterAtom, skjulFilter }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [accordionsOpen, setAccordionsOpen] = useAtom(gjennomforingFilterAccordionAtom);
  const { data: avtale } = useAvtale();
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const { data: virksomheter, isLoading: isLoadingVirksomheter } = useVirksomheter(
    VirksomhetTil.TILTAKSGJENNOMFORING,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1,
  );

  useEffect(() => {
    setFilter({
      ...filter,
      avtale: avtale?.id ?? "",
    });
  }, [avtale]);

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
        <Skeleton height={50} variant="rounded" />
      </VStack>
    );
  }

  return (
    <>
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
            search,
          });
        }}
        value={filter.search}
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
              items={TILTAKSGJENNOMFORING_STATUS_OPTIONS}
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
        <Accordion.Item open={accordionsOpen.includes("enhet")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "enhet")]);
            }}
          >
            Enhet
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              searchable
              items={enhetOptions(enheter, filter.navRegioner)}
              isChecked={(enhet) => filter.navEnheter.includes(enhet)}
              onChange={(enhet) => {
                setFilter({
                  ...filter,
                  page: 1,
                  navEnheter: addOrRemove(filter.navEnheter, enhet),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("arrangor")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "arrangor")]);
            }}
          >
            Arrangør
          </Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              searchable
              items={virksomhetOptions(virksomheter)}
              isChecked={(orgnr) => filter.arrangorOrgnr.includes(orgnr)}
              onChange={(orgnr) => {
                setFilter({
                  ...filter,
                  page: 1,
                  arrangorOrgnr: addOrRemove(filter.arrangorOrgnr, orgnr),
                });
              }}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </>
  );
}

interface CheckboxListProps<T> {
  items: { label: string; value: T }[];
  isChecked: (a: T) => boolean;
  onChange: (a: T) => void;
  searchable?: boolean;
}

export function CheckboxList<T>(props: CheckboxListProps<T>) {
  const { items, isChecked, onChange, searchable = false } = props;
  const [search, setSearch] = useState<string>("");

  return (
    <div className={styles.checkbox_list}>
      {searchable && (
        <Search
          label=""
          size="small"
          variant="simple"
          onChange={(v: string) => setSearch(v)}
          value={search}
          className={styles.checkbox_search}
        />
      )}
      {items
        .filter((item) => item.label.toLocaleLowerCase().includes(search.toLocaleLowerCase()))
        .map((item) => (
          <Checkbox
            key={item.value as string}
            size="small"
            onChange={() => onChange(item.value)}
            checked={isChecked(item.value)}
          >
            {item.label}
          </Checkbox>
        ))}
    </div>
  );
}
