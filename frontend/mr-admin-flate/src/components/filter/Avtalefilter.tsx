import { Accordion, Search, Skeleton } from "@navikt/ds-react";
import { WritableAtom, useAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { AvtaleFilterProps } from "src/api/atoms";
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
    return <Skeleton variant="rounded" height="400px" />;
  }

  return (
    <div>
      <Search
        label="Søk etter tiltaksgjennomføring"
        hideLabel
        size="small"
        variant="simple"
        placeholder="Navn eller tiltaksnr."
        onChange={(search: string) =>
          setFilter({
            ...filter,
            sok: search,
          })
        }
        value={filter.sok}
        aria-label="Søk etter tiltaksgjennomføring"
      />
      <Accordion>
        <Accordion.Item>
          <Accordion.Header>Status</Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={AVTALE_STATUS_OPTIONS}
              isChecked={(status) => filter.statuser.includes(status)}
              onChange={(status) =>
                setFilter({
                  ...filter,
                  statuser: addOrRemove(filter.statuser, status),
                })
              }
            />
          </Accordion.Content>
        </Accordion.Item>
        {!skjulFilter?.tiltakstype && (
          <Accordion.Item>
            <Accordion.Header>Tiltakstype</Accordion.Header>
            <Accordion.Content>
              <CheckboxList
                items={tiltakstypeOptions(tiltakstyper.data)}
                isChecked={(tiltakstype) => filter.tiltakstyper.includes(tiltakstype)}
                onChange={(tiltakstype) =>
                  setFilter({
                    ...filter,
                    tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                  })
                }
              />
            </Accordion.Content>
          </Accordion.Item>
        )}
        <Accordion.Item>
          <Accordion.Header>Region</Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              items={regionOptions(enheter)}
              isChecked={(region) => filter.navRegioner.includes(region)}
              onChange={(region) =>
                setFilter({
                  ...filter,
                  navRegioner: addOrRemove(filter.navRegioner, region),
                })
              }
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item>
          <Accordion.Header>Leverandør</Accordion.Header>
          <Accordion.Content>
            <CheckboxList
              searchable
              items={virksomhetOptions(virksomheter)}
              isChecked={(orgnr) => filter.leverandor_orgnr.includes(orgnr)}
              onChange={(orgnr) =>
                setFilter({
                  ...filter,
                  leverandor_orgnr: addOrRemove(filter.leverandor_orgnr, orgnr),
                })
              }
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
