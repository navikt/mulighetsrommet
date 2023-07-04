import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  Norg2Type,
  TiltaksgjennomforingStatus,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  Tiltaksgjennomforingfilter as TiltaksgjennomforingAtomFilter,
  defaultTiltaksgjennomforingfilter,
  paginationAtom,
  tiltaksgjennomforingfilter,
} from "../../api/atoms";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import {
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import {
  inneholderUrl,
  resetPaginering,
  valueOrDefault,
} from "../../utils/Utils";
import { arenaKodeErAftEllerVta } from "../../utils/tiltakskoder";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";
import { RESET } from "jotai/vanilla/utils";
import { useNavigate } from "react-router-dom";
import classNames from "classnames";
import { FilterTag } from "./FilterTag";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAlleUtkast } from "../../api/utkast/useAlleUtkast";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
  avtale?: Avtale;
}

export function Tiltaksgjennomforingfilter({ skjulFilter, avtale }: Props) {
  const navigate = useNavigate();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);
  const [, setPage] = useAtom(paginationAtom);
  const { data: enheter } = useAlleEnheter();
  const { data: virksomheter } = useVirksomheter(
    VirksomhetTil.TILTAKSGJENNOMFORING
  );
  const { data: tiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1
  );
  const [opprettModal, setOpprettModalOpen] = useState<boolean>(false);
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const { data: brukerData } = useHentAnsatt();
  const { refetch: refetchUtkast } = useAlleUtkast(
    avtale?.id,
    brukerData?.navIdent
  );

  const form = useForm<TiltaksgjennomforingAtomFilter>({
    defaultValues: {
      ...filter,
    },
  });
  const { register, setValue } = form;

  useEffect(() => {
    // Reset filter når vi unmounter
    return () => setFilter(RESET);
  }, []);

  const features = useFeatureToggles();
  const visOpprettTiltaksgjennomforingKnapp =
    features.isSuccess &&
    features.data[OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE] &&
    inneholderUrl("/avtaler/");

  const erAFTellerVTA = arenaKodeErAftEllerVta(avtale?.tiltakstype.arenaKode);

  const regionOptions = () => {
    const options =
      enheter
        ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
        ?.sort()
        ?.map((enhet) => ({ label: enhet.navn, value: enhet.enhetsnummer })) ||
      [];

    return [{ value: "", label: "Alle regioner" }, ...options];
  };

  const enhetOptions = () => {
    const options =
      enheter
        ?.filter((enhet) => {
          const erLokalEllerTiltaksenhet =
            enhet.type === Norg2Type.LOKAL || enhet.type === Norg2Type.TILTAK;
          const enheterFraFylke =
            filter.navRegion === ""
              ? true
              : filter.navRegion === enhet.overordnetEnhet;
          return erLokalEllerTiltaksenhet && enheterFraFylke;
        })
        ?.sort()
        ?.map((enhet) => ({
          label: `${enhet.navn} - ${enhet.enhetsnummer}`,
          value: enhet.enhetsnummer,
        })) || [];

    return [{ value: "", label: "Alle enheter" }, ...options];
  };

  const tiltakstypeOptions = () => {
    const options =
      tiltakstyper?.data?.sort()?.map((tiltakstype) => ({
        label: tiltakstype.navn,
        value: tiltakstype.id,
      })) || [];

    return [{ value: "", label: "Alle tiltakstyper" }, ...options];
  };

  const statusOptions = () => {
    return [
      { label: "Gjennomføres", value: TiltaksgjennomforingStatus.GJENNOMFORES },
      { label: "Avsluttet", value: TiltaksgjennomforingStatus.AVSLUTTET },
      { label: "Avbrutt", value: TiltaksgjennomforingStatus.AVBRUTT },
      {
        label: "Åpent for innsøk",
        value: TiltaksgjennomforingStatus.APENT_FOR_INNSOK,
      },
      { label: "Alle statuser", value: "" },
    ];
  };

  const arrangorOptions = () => {
    const options =
      virksomheter?.sort()?.map((virksomhet) => ({
        label: virksomhet.navn,
        value: virksomhet.organisasjonsnummer,
      })) || [];

    return [{ value: "", label: "Alle arrangører" }, ...options];
  };

  return (
    <FormProvider {...form}>
      <form
        className={
          avtale
            ? styles.tiltaksgjennomforingform_med_knapperad
            : styles.tiltaksgjennomforingform
        }
      >
        <div className={styles.filter_container}>
          <div className={styles.filtrering}>
            <Search
              label="Søk etter tiltaksgjennomføring"
              hideLabel
              size="small"
              variant="simple"
              onChange={(search: string) => setFilter({ ...filter, search })}
              value={filter.search}
              aria-label="Søk etter tiltaksgjennomføring"
              data-testid="filter_sokefelt"
              className={styles.form_field}
            />
            <SokeSelect
              size="small"
              label="Filtrer på region"
              placeholder="Filtrer på region"
              hideLabel
              {...register("navRegion")}
              onChange={(navRegion) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  enhet: "",
                  navRegion: valueOrDefault(
                    navRegion,
                    defaultTiltaksgjennomforingfilter.navRegion
                  ),
                });
              }}
              options={regionOptions()}
              className={styles.form_field}
            />

            <SokeSelect
              size="small"
              label="Filtrer på enhet"
              placeholder="Filtrer på enhet"
              hideLabel
              {...register("enhet")}
              onChange={(enhet) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  enhet: valueOrDefault(
                    enhet,
                    defaultTiltaksgjennomforingfilter.enhet
                  ),
                });
              }}
              options={enhetOptions()}
              className={styles.form_field}
            />
            {skjulFilter?.tiltakstype ? null : (
              <SokeSelect
                size="small"
                label="Filtrer på tiltakstype"
                placeholder="Filtrer på tiltakstype"
                hideLabel
                {...register("tiltakstype")}
                onChange={(tiltakstype) => {
                  resetPaginering(setPage);
                  setFilter({
                    ...filter,
                    tiltakstype: valueOrDefault(
                      tiltakstype,
                      defaultTiltaksgjennomforingfilter.tiltakstype
                    ),
                  });
                }}
                options={tiltakstypeOptions()}
                className={styles.form_field}
              />
            )}
            <SokeSelect
              size="small"
              label="Filtrer på status"
              placeholder="Filtrer på status"
              hideLabel
              {...register("status")}
              onChange={(status) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  status: valueOrDefault(
                    status,
                    defaultTiltaksgjennomforingfilter.status
                  ),
                });
              }}
              options={statusOptions()}
              className={styles.form_field}
            />
            <SokeSelect
              size="small"
              label="Filtrer på arrangør"
              placeholder="Filtrer på arrangør"
              hideLabel
              {...register("arrangorOrgnr")}
              onChange={(arrangorOrgnr) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  arrangorOrgnr: valueOrDefault(
                    arrangorOrgnr,
                    defaultTiltaksgjennomforingfilter.arrangorOrgnr
                  ),
                });
              }}
              options={arrangorOptions()}
              className={styles.form_field}
            />
          </div>

          {avtale && (
            <div
              className={classNames(
                styles.knapperad,
                styles.tiltaksgjennomforings_knapperad
              )}
            >
              <>
                {visOpprettTiltaksgjennomforingKnapp && (
                  <>
                    <Button
                      size="small"
                      onClick={() => setOpprettModalOpen(true)}
                      type="button"
                    >
                      Opprett ny gjennomføring
                    </Button>

                    <OpprettTiltaksgjennomforingModal
                      modalOpen={opprettModal}
                      avtale={avtale}
                      onClose={() => {
                        refetchUtkast();
                        setOpprettModalOpen(false);
                      }}
                      onSuccess={(id) =>
                        navigate(`/tiltaksgjennomforinger/${id}`)
                      }
                    />
                  </>
                )}
                {erAFTellerVTA && (
                  <>
                    <Button
                      size="small"
                      onClick={() => setModalOpen(true)}
                      variant="secondary"
                      type="button"
                    >
                      Legg til gjennomføring
                    </Button>
                    <LeggTilGjennomforingModal
                      modalOpen={modalOpen}
                      onClose={() => setModalOpen(false)}
                    />
                  </>
                )}
              </>
            </div>
          )}
          <div className={styles.tags_container}>
            {filter.navRegion && (
              <FilterTag
                label={
                  enheter?.find((e) => e.enhetsnummer === filter.navRegion)
                    ?.navn
                }
                onClick={() => {
                  setFilter({
                    ...filter,
                    navRegion: defaultTiltaksgjennomforingfilter.navRegion,
                  });
                  setValue(
                    "navRegion",
                    defaultTiltaksgjennomforingfilter.navRegion
                  );
                }}
              />
            )}
            {filter.enhet && (
              <FilterTag
                label={
                  enheter?.find((e) => e.enhetsnummer === filter.enhet)?.navn
                }
                onClick={() => {
                  setFilter({
                    ...filter,
                    enhet: defaultTiltaksgjennomforingfilter.enhet,
                  });
                  setValue("enhet", defaultTiltaksgjennomforingfilter.enhet);
                }}
              />
            )}
            {filter.tiltakstype && (
              <FilterTag
                label={
                  tiltakstyper?.data?.find((t) => t.id === filter.tiltakstype)
                    ?.navn
                }
                onClick={() => {
                  setFilter({
                    ...filter,
                    tiltakstype: defaultTiltaksgjennomforingfilter.tiltakstype,
                  });
                  setValue(
                    "tiltakstype",
                    defaultTiltaksgjennomforingfilter.tiltakstype
                  );
                }}
              />
            )}
            {filter.status && (
              <FilterTag
                label={
                  statusOptions().find((o) => o.value === filter.status)?.label
                }
                onClick={() => {
                  setFilter({ ...filter, status: "" });
                  setValue("status", "");
                }}
              />
            )}
            {filter.arrangorOrgnr && (
              <FilterTag
                label={
                  virksomheter?.find(
                    (v) => v.organisasjonsnummer === filter.arrangorOrgnr
                  )?.navn
                }
                onClick={() => {
                  setFilter({
                    ...filter,
                    arrangorOrgnr:
                      defaultTiltaksgjennomforingfilter.arrangorOrgnr,
                  });
                  setValue(
                    "arrangorOrgnr",
                    defaultTiltaksgjennomforingfilter.arrangorOrgnr
                  );
                }}
              />
            )}
            {(filter.navRegion ||
              filter.enhet ||
              filter.tiltakstype ||
              filter.status ||
              filter.arrangorOrgnr) && (
              <Button
                type="button"
                size="small"
                variant="tertiary"
                onClick={() => {
                  setFilter({ ...defaultTiltaksgjennomforingfilter, status: "", avtale: filter.avtale });
                  setValue('status', "");
                  setValue('enhet', defaultTiltaksgjennomforingfilter.enhet);
                  setValue('navRegion', defaultTiltaksgjennomforingfilter.navRegion);
                  setValue('tiltakstype', defaultTiltaksgjennomforingfilter.tiltakstype);
                  setValue('arrangorOrgnr', defaultTiltaksgjennomforingfilter.arrangorOrgnr);
                }}
              >
                Tilbakestill filter
              </Button>
            )}
          </div>
        </div>
      </form>
    </FormProvider>
  );
}
