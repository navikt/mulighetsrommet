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
import { FieldError } from "api-client";
import { useEffect, useRef, useState } from "react";
import { Link as ReactRouterLink, MetaFunction, useNavigate, useLocation } from "react-router";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { tekster } from "~/tekster";
import { UtbetalingManglendeTilsagnAlert } from "~/components/utbetaling/UtbetalingManglendeTilsagnAlert";
import { pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { errorAt } from "~/utils/validering";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { SatsPerioderOgBelop } from "~/components/utbetaling/SatsPerioderOgBelop";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useArrangorflateTilsagnTilUtbetaling } from "~/hooks/useArrangorflateTilsagnTilUtbetaling";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useSyncKontonummer } from "~/hooks/useSyncKontonummer";
import { useGodkjennUtbetaling } from "~/hooks/useGodkjennUtbetaling";
import { queryKeys } from "~/api/queryKeys";
import { useQueryClient } from "@tanstack/react-query";

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
  const queryClient = useQueryClient();
  const { updatedAt } = useLocation().state;

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const { data: tilsagn } = useArrangorflateTilsagnTilUtbetaling(id);
  const syncKontonummer = useSyncKontonummer(id);
  const godkjennUtbetaling = useGodkjennUtbetaling();

  const [kid, setKid] = useState(utbetaling.betalingsinformasjon?.kid ?? "");
  const [bekreftelse, setBekreftelse] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  const errorSummaryRef = useRef<HTMLDivElement>(null);
  const hasError = errors.length > 0;

  const handleHentKontonummer = () => {
    syncKontonummer.mutate();
  };

  const handleSubmit = async (e: React.FormEvent) => {
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
    });

    if (result.errors) {
      setErrors(result.errors);
    } else if (result.success) {
      queryClient.invalidateQueries({ queryKey: queryKeys.utbetaling(utbetaling.id) });
      navigate(pathTo.kvittering(orgnr, id));
    }
  };

  useEffect(() => {
    if (hasError) {
      errorSummaryRef.current?.focus();
    }
  }, [hasError]);

  const harTilsagn = tilsagn.length > 0;

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
        ]}
      />
      <Separator />
      <Definisjonsliste
        title={"Utbetaling"}
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
        ]}
      />
      <SatsPerioderOgBelop
        pris={utbetaling.beregning.pris}
        satsDetaljer={utbetaling.beregning.satsDetaljer}
      />
      <Separator />
      <form onSubmit={handleSubmit}>
        <Box marginBlock="space-0 space-16">
          {harTilsagn ? (
            <>
              <Heading size="medium" level="3" spacing>
                Betalingsinformasjon
              </Heading>
              <VStack gap="space-16">
                <KontonummerInput
                  kontonummer={utbetaling.betalingsinformasjon?.kontonummer ?? undefined}
                  error={errors.find((error) => error.pointer === "/kontonummer")?.detail}
                  onClick={() => handleHentKontonummer()}
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
              <CheckboxGroup error={errorAt("/bekreftelse", errors)} legend="Bekreftelse">
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
            </>
          ) : (
            <UtbetalingManglendeTilsagnAlert />
          )}
        </Box>
        <HStack gap="space-16">
          <Button
            as={ReactRouterLink}
            type="button"
            variant="tertiary"
            to={pathTo.beregning(orgnr, utbetaling.id)}
          >
            Tilbake
          </Button>
          {harTilsagn && (
            <Button type="submit" loading={godkjennUtbetaling.isPending}>
              Bekreft og send inn
            </Button>
          )}
        </HStack>
      </form>
    </>
  );
}
