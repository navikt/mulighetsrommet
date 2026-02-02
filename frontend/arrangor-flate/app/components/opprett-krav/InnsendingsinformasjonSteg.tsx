import {
  ArrangorflateTilsagnDto,
  DatoVelger,
  FieldError,
  GuidePanelType,
  OpprettKravInnsendingSteg,
  Periode,
} from "@api-client";
import {
  Alert,
  BodyShort,
  DatePicker,
  GuidePanel,
  Heading,
  HStack,
  Label,
  Link,
  Select,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { SyntheticEvent, useCallback, useEffect, useMemo, useState } from "react";
import { OpprettKravFormState } from "~/routes/$orgnr.opprett-krav.$gjennomforingid";
import { filtrerOverlappendePerioder } from "~/utils/periode-filtrering";
import { TilsagnDetaljer } from "../tilsagn/TilsagnDetaljer";
import { LabeledDataElementList } from "../common/Definisjonsliste";
import { Link as ReactRouterLink } from "react-router";
import { errorAt } from "~/utils/validering";
import {
  addDuration,
  formaterPeriode,
  parseDate,
  subDuration,
  yyyyMMddFormatting,
} from "@mr/frontend-common/utils/date";
import { pathTo } from "~/utils/navigation";

interface InnsendingsinformasjonStepProps {
  data: OpprettKravInnsendingSteg;
  formState: OpprettKravFormState;
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
  errors: FieldError[];
}

export default function InnsendingsinformasjonSteg({
  data,
  formState,
  updateFormState,
  errors,
}: InnsendingsinformasjonStepProps) {
  const [valgtPeriode, setValgtPeriode] = useState<Periode | undefined>(() => {
    if (formState.periodeStart && formState.periodeSlutt) {
      return { start: formState.periodeStart, slutt: formState.periodeSlutt };
    }
    return undefined;
  });

  const relevanteTilsagn = useMemo(() => {
    if (!valgtPeriode) return [];
    return filtrerOverlappendePerioder<ArrangorflateTilsagnDto>(valgtPeriode, data.tilsagn);
  }, [data.tilsagn, valgtPeriode]);

  const handlePeriodeSelected = useCallback(
    (periode?: Periode) => {
      setValgtPeriode(periode);
      if (periode) {
        updateFormState({
          periodeStart: periode.start,
          periodeSlutt: periode.slutt,
          tilsagnId: undefined,
        });
      } else {
        updateFormState({ periodeStart: undefined, periodeSlutt: undefined, tilsagnId: undefined });
      }
    },
    [updateFormState],
  );

  useEffect(() => {
    if (relevanteTilsagn.length > 0) {
      updateFormState({ tilsagnId: relevanteTilsagn[0].id });
    }
  }, [relevanteTilsagn, updateFormState]);

  return (
    <>
      <Heading level="3" size="large">
        Innsendingsinformasjon
      </Heading>
      <GuidePanelInformation type={data.guidePanel} />
      <VStack gap="6" className="max-w-2xl">
        <LabeledDataElementList entries={data.definisjonsListe} />
        <VStack gap="1">
          <Label size="small">Periode</Label>
          <BodyShort textColor="subtle" size="small">
            Hvilken periode gjelder kravet for?
          </BodyShort>
          <PeriodeVelgerVarianter
            onPeriodeSelected={handlePeriodeSelected}
            type={data.datoVelger}
            sessionPeriodeStart={formState.periodeStart}
            sessionPeriodeSlutt={formState.periodeSlutt}
            errors={errors}
            updateFormState={updateFormState}
          />
        </VStack>
        {valgtPeriode && (
          <>
            <Heading level="3" size="small">
              Tilgjengelige tilsagn
            </Heading>
            <BodyShort size="small" textColor="subtle">
              Under vises informasjon om antatt forbruk.
              <br />
              Hva som blir utbetalt avhenger imidlertid av faktisk forbruk i perioden.
            </BodyShort>
            {relevanteTilsagn.length < 1 ? (
              <Alert variant="warning">
                Fant ingen aktive tilsagn for gjennomføringen. Vennligst ta kontakt med Nav.
              </Alert>
            ) : (
              <>
                {relevanteTilsagn.map((tilsagn) => (
                  <TilsagnDetaljer key={tilsagn.id} tilsagn={tilsagn} minimal />
                ))}
              </>
            )}
          </>
        )}
      </VStack>
    </>
  );
}

interface GuidePanelInformationProps {
  type: GuidePanelType | null;
}

function GuidePanelInformation({ type }: GuidePanelInformationProps) {
  switch (type) {
    case GuidePanelType.INVESTERING_VTA_AFT:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn krav som gjelder tilsagn for investeringer. Andre krav
          om utbetaling skal sendes inn via utbetalingene i{" "}
          <Link as={ReactRouterLink} to={pathTo.utbetalinger}>
            Utbetalingsoversikten.
          </Link>
        </GuidePanel>
      );
    case GuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav for tiltak med avtalt timespris
        </GuidePanel>
      );
    case GuidePanelType.AVTALT_PRIS:
      return (
        <GuidePanel>
          I dette skjemaet kan du sende inn fakturakrav i henhold til avtalt pris med Nav
        </GuidePanel>
      );
    case null:
      return null;
  }
}

interface PeriodeVelgerVarianterProps {
  onPeriodeSelected: (periode?: Periode) => void;
  type: DatoVelger;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
}

function PeriodeVelgerVarianter({
  onPeriodeSelected,
  type,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
  updateFormState,
}: PeriodeVelgerVarianterProps) {
  switch (type.type) {
    case "DatoVelgerSelect":
      return (
        <PeriodeSelect
          periodeForslag={type.periodeForslag}
          onPeriodeSelected={onPeriodeSelected}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
        />
      );
    case "DatoVelgerRange":
      return (
        <PeriodeVelger
          maksSluttdato={type.maksSluttdato}
          onPeriodeSelected={onPeriodeSelected}
          sessionPeriodeStart={sessionPeriodeStart}
          sessionPeriodeSlutt={sessionPeriodeSlutt}
          errors={errors}
          updateFormState={updateFormState}
        />
      );
    case undefined:
      throw Error("Ugyldig DatoVelger variant");
  }
}

interface PeriodeSelectProps {
  onPeriodeSelected: (periode?: Periode) => void;
  periodeForslag: Array<Periode>;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
}

function PeriodeSelect({
  onPeriodeSelected,
  periodeForslag,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
}: PeriodeSelectProps) {
  function onChange(e: SyntheticEvent<HTMLSelectElement, Event>) {
    const selectedValue = (e.target as HTMLSelectElement).value;
    if (!selectedValue) {
      onPeriodeSelected();
      return;
    }
    const selectedPeriode = periodeForslag[Number(selectedValue)];
    onPeriodeSelected(selectedPeriode);
  }

  return (
    <HStack gap="4">
      <Select
        hideLabel
        label="Hvilken periode gjelder kravet for?"
        size="small"
        name="periode"
        defaultValue={periodeForslag.findIndex(
          (periode) =>
            periode.start === sessionPeriodeStart && periode.slutt === sessionPeriodeSlutt,
        )}
        onChange={onChange}
      >
        <option value="">Velg periode</option>
        {periodeForslag.map((periode, index) => (
          <option key={periode.start} value={index}>
            {formaterPeriode(periode)}
          </option>
        ))}
      </Select>
    </HStack>
  );
}

interface PeriodeVelgerProps {
  maksSluttdato: string;
  onPeriodeSelected: (periode?: Periode) => void;
  sessionPeriodeStart?: string;
  sessionPeriodeSlutt?: string;
  errors?: FieldError[];
  updateFormState: (updates: Partial<OpprettKravFormState>) => void;
}

function PeriodeVelger({
  maksSluttdato,
  onPeriodeSelected,
  sessionPeriodeStart,
  sessionPeriodeSlutt,
  errors,
  updateFormState,
}: PeriodeVelgerProps) {
  const sisteDato = subDuration(maksSluttdato, { days: 1 });
  const {
    datepickerProps: periodeStartPickerProps,
    inputProps: periodeStartInputProps,
    selectedDay: selectedStartDato,
  } = useDatepicker({
    defaultSelected: parseDate(sessionPeriodeStart),
    toDate: sisteDato,
  });
  const {
    datepickerProps: periodeSluttPickerProps,
    inputProps: periodeSluttInputProps,
    selectedDay: selectedSluttDato,
  } = useDatepicker({
    defaultSelected: parseDate(sessionPeriodeSlutt),
    toDate: sisteDato,
  });

  useEffect(() => {
    if (selectedStartDato && selectedSluttDato) {
      const start = yyyyMMddFormatting(selectedStartDato)!;
      const slutt = yyyyMMddFormatting(addDuration(selectedSluttDato, { days: 1 }))!;
      updateFormState({ periodeInklusiv: true });
      onPeriodeSelected({ start, slutt });
    } else {
      onPeriodeSelected();
    }
  }, [selectedStartDato, selectedSluttDato, onPeriodeSelected, updateFormState]);

  return (
    <HStack wrap gap="4">
      <DatePicker
        {...periodeStartPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeStartDatepicker"
      >
        <DatePicker.Input
          label="Fra dato"
          size="small"
          error={errorAt("/periodeStart", errors)}
          name="periodeStart"
          id="periodeStart"
          {...periodeStartInputProps}
        />
      </DatePicker>
      <DatePicker
        {...periodeSluttPickerProps}
        showWeekNumber
        dropdownCaption
        id="periodeSluttDatepicker"
      >
        <DatePicker.Input
          label="Til dato"
          size="small"
          error={errorAt("/periodeSlutt", errors)}
          name="periodeSlutt"
          id="periodeSlutt"
          {...periodeSluttInputProps}
        />
      </DatePicker>
    </HStack>
  );
}
