import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import {
  Box,
  Button,
  Checkbox,
  CheckboxGroup,
  ErrorSummary,
  Heading,
  HStack,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { FieldError } from "@arrangor-utbetalinger/api-client";
import { SubmitEvent, useEffect, useRef, useState } from "react";
import { MetaFunction, useLocation, useNavigate } from "react-router";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { errorAt } from "~/utils/validering";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useSyncKontonummer } from "~/hooks/useSyncKontonummer";
import { useGodkjennUtbetaling } from "~/hooks/useGodkjennUtbetaling";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { BlokkeringerVarsler } from "~/components/common/BlokkeringerVarsler";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 3 av 3: Oppsummering - Godkjenn innsending" },
    {
      name: "description",
      content: "Oppsummering av innsendingen og betalingsinformasjon",
    },
  ];
};

export default function BekreftUtbetaling() {
  const id = useIdFromUrl();
  const orgnr = useOrgnrFromUrl();
  const navigate = useNavigate();
  const { updatedAt, belop, vedlegg } = useLocation().state || {};

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const syncKontonummer = useSyncKontonummer(id);
  const godkjennUtbetaling = useGodkjennUtbetaling();

  const wizard = useUtbetalingWizard(utbetaling);

  const [kid, setKid] = useState(utbetaling.betalingsinformasjon?.kid ?? "");
  const [bekreftelse, setBekreftelse] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = errors.length > 0;

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [hasError]);

  const handleSubmit = async (e: SubmitEvent) => {
    e.preventDefault();

    const newErrors: FieldError[] = [];

    if (!bekreftelse) {
      newErrors.push({
        pointer: "/bekreftelse",
        detail: "Du må bekrefte at opplysningene er korrekte",
      });
    }
    if (!utbetaling.betalingsinformasjon?.kontonummer) {
      newErrors.push({
        pointer: "/kontonummer",
        detail: "Kontonummer eksisterer ikke",
      });
    }

    if (newErrors.length > 0) {
      setErrors(newErrors);
      return;
    }

    const result = await godkjennUtbetaling.mutateAsync({
      id: id,
      updatedAt: updatedAt,
      kid: kid || null,
      belop: utbetaling.kanRegistrerePris && belop != null ? belop : null,
      vedlegg: utbetaling.kanRegistrerePris && vedlegg ? vedlegg : null,
    });

    if (result.errors) {
      setErrors(result.errors);
    } else if (result.success) {
      navigate(pathTo.kvittering(orgnr, id));
    }
  };

  return (
    <>
      <Heading level="2" spacing size="large">
        Oppsummering
      </Heading>
      <Definisjonsliste
        title="Innsendingsinformasjon"
        definitions={[
          {
            key: "Arrangør",
            value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
          },
          {
            key: "Tiltaksnavn",
            value: `${utbetaling.gjennomforing.navn} (${utbetaling.gjennomforing.lopenummer})`,
          },
          { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
        ]}
      />
      <Separator />
      {utbetaling.kanRegistrerePris ? (
        <VStack gap="space-4">
          <Definisjonsliste
            definitions={[
              { key: "Beløp", value: `${belop} kr` },
              {
                key: "Vedlegg",
                value: (vedlegg ?? []).map((file: File) => file.name).join(", "),
              },
            ]}
          />
        </VStack>
      ) : (
      <SatsPerioderOgBelop
          pris={utbetaling.beregning.pris}
          satsDetaljer={utbetaling.beregning.satsDetaljer}
        />
      )}
      <Separator />
      <form onSubmit={handleSubmit}>
        <Box marginBlock="space-0 space-16">
          <Heading size="medium" level="3" spacing>
            Betalingsinformasjon
          </Heading>
          <VStack gap="space-16">
            <KontonummerInput
              kontonummer={utbetaling.betalingsinformasjon?.kontonummer ?? undefined}
              error={errors.find((error) => error.pointer === "/kontonummer")?.detail}
              onClick={() => syncKontonummer.mutate()}
            />
            <TextField
              label="KID-nummer for utbetaling (valgfritt)"
              size="small"
              name="kid"
              htmlSize={35}
              error={errors.find((error) => error.pointer === "/kid")?.detail}
              value={kid}
              onChange={(e) => setKid(e.target.value)}
              maxLength={25}
              id="kid"
            />
          </VStack>
          <Separator />
          <Heading size="medium" level="3">
            Bekreftelse
          </Heading>
          <CheckboxGroup error={errorAt("/bekreftelse", errors)} hideLegend legend="Bekreftelse">
            <Checkbox
              name="bekreftelse"
              value="bekreftet"
              id="bekreftelse"
              checked={bekreftelse}
              onChange={(e) => setBekreftelse(e.target.checked)}
              error={errorAt("/bekreftelse", errors) !== undefined}
            >
              {tekster.bokmal.utbetaling.oppsummering.bekreftelse}
            </Checkbox>
            <Separator />
            <BlokkeringerVarsler
              blokkeringer={utbetaling.blokkeringer}
              advarsler={utbetaling.advarsler}
            />
          </CheckboxGroup>
          {hasError && (
            <ErrorSummary ref={errorSummaryRef}>
              {errors.map((error: FieldError) => {
                return (
                  <ErrorSummary.Item
                    href={`#${jsonPointerToFieldPath(error.pointer)}`}
                    key={jsonPointerToFieldPath(error.pointer)}
                  >
                    {error.detail}
                  </ErrorSummary.Item>
                );
              })}
            </ErrorSummary>
          )}
        </Box>
        <HStack gap="space-16">
          <Button type="button" variant="tertiary" onClick={wizard.goToPrevious}>
            Tilbake
          </Button>
          {utbetaling.blokkeringer.length === 0 && (
            <Button type="submit" loading={godkjennUtbetaling.isPending}>
              Bekreft og send inn
            </Button>
          )}
        </HStack>
      </form>
    </>
  );
}
