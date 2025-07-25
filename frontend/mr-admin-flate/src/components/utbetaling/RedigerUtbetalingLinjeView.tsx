import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { utbetalingLinjeCompareFn } from "@/utils/Utils";
import {
  DelutbetalingRequest,
  FieldError,
  OpprettDelutbetalingerRequest,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
  Tilskuddstype,
  Toggles,
  UtbetalingDto,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { FileCheckmarkIcon, InformationSquareFillIcon, PiggybankIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Button,
  Heading,
  HStack,
  Modal,
  Spacer,
  Textarea,
  VStack,
} from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { ChangeEventHandler, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useAvbrytUtbetaling } from "@/api/utbetaling/useAvbrytUtbetaling";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  tilsagn: TilsagnDto[];
}

function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status === TilsagnStatus.GODKJENT)
    .map((t) => ({
      belop: 0,
      tilsagn: t,
      gjorOppTilsagn: false,
      id: uuidv4(),
    }))
    .toSorted(utbetalingLinjeCompareFn);
}

export function RedigerUtbetalingLinjeView({ linjer, utbetaling, tilsagn }: Props) {
  const { gjennomforingId } = useParams();
  const [linjerState, setLinjerState] = useState<UtbetalingLinje[]>(() =>
    linjer.length === 0 ? genrererUtbetalingLinjer(tilsagn) : linjer,
  );

  const { data: enableAvbrytUtbetaling } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_MIGRERING_OKONOMI_AVBRYT_UTBETALING,
  );

  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [error, setError] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  const avbrytMutation = useAvbrytUtbetaling();

  function avbryt(aarsaker: string[], forklaring: string | null) {
    avbrytMutation.mutate(
      {
        id: utbetaling.id,
        body: { aarsaker, forklaring },
      },
      {
        onValidationError: (error: ValidationError) => {
          setError(error.errors);
        },
        onSuccess: () => {
          navigate(-1);
        },
      },
    );
  }

  const tilsagnsTypeFraTilskudd = tilsagnType(utbetaling.tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop = tilsagn.length === 0 ? utbetaling.belop : 0;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&belop=${defaultBelop}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  function leggTilLinjer() {
    const nyeLinjer = genrererUtbetalingLinjer(tilsagn).filter(
      (linje) => !linjerState.find((l) => l.tilsagn.id === linje.tilsagn.id),
    );
    setLinjerState([...linjerState, ...nyeLinjer].toSorted(utbetalingLinjeCompareFn));
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjerState.map((linje) => {
      return {
        id: linje.id,
        belop: linje.belop,
        gjorOppTilsagn: linje.gjorOppTilsagn,
        tilsagnId: linje.tilsagn.id,
      };
    });

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
      begrunnelseMindreBetalt,
    };

    setError([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
      },
      onValidationError: (error: ValidationError) => {
        setError(error.errors);
      },
    });
  }

  function fjernLinje(id: string) {
    setError([]);
    const remaining = linjerState.filter((d) => d.id !== id);
    setLinjerState([...remaining]);
  }

  function utbetalesTotal(): number {
    return linjerState.reduce((acc, d) => acc + d.belop, 0);
  }

  return (
    <>
      <VStack>
        <HStack align="end">
          <Heading spacing size="medium" level="2">
            Utbetalingslinjer
          </Heading>
          <Spacer />
          <ActionMenu>
            <ActionMenu.Trigger>
              <Button variant="secondary" size="small">
                Handlinger
              </Button>
            </ActionMenu.Trigger>
            <ActionMenu.Content>
              <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                Opprett {avtaletekster.tilsagn.type(tilsagnsTypeFraTilskudd).toLowerCase()}
              </ActionMenu.Item>
              <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={leggTilLinjer}>
                Hent godkjente tilsagn
              </ActionMenu.Item>
              {enableAvbrytUtbetaling && (
                <ActionMenu.Item onSelect={() => setAvbrytModalOpen(true)}>
                  Avbryt utbetaling
                </ActionMenu.Item>
              )}
            </ActionMenu.Content>
          </ActionMenu>
        </HStack>
        <UtbetalingLinjeTable
          utbetaling={utbetaling}
          linjer={linjerState}
          renderRow={(linje, index) => {
            return (
              <UtbetalingLinjeRow
                key={linje.id}
                linje={linje}
                knappeColumn={
                  <Button
                    size="small"
                    variant="primary-neutral"
                    onClick={() => fjernLinje(linje.id)}
                  >
                    Fjern
                  </Button>
                }
                grayBackground
                onChange={(updated) => {
                  setLinjerState((prev) =>
                    prev.map((linje) => (linje.id === updated.id ? updated : linje)),
                  );
                }}
                errors={error.filter(
                  (f) => f.pointer.startsWith(`/${index}`) || f.pointer.includes("totalbelop"),
                )}
              />
            );
          }}
        />
      </VStack>
      <VStack align="end" gap="4">
        <HStack>
          <Button
            size="small"
            type="button"
            onClick={() => {
              if (utbetalesTotal() < utbetaling.belop) {
                setMindreBelopModalOpen(true);
              } else {
                sendTilGodkjenning();
              }
            }}
          >
            Send til attestering
          </Button>
        </HStack>
        {error.find((f) => f.pointer === "/") && (
          <Alert variant="error" size="small">
            {error.find((f) => f.pointer === "/")!.detail}
          </Alert>
        )}
      </VStack>
      <MindreBelopModal
        open={mindreBelopModalOpen}
        handleClose={() => setMindreBelopModalOpen(false)}
        onConfirm={() => {
          setMindreBelopModalOpen(false);
          sendTilGodkjenning();
        }}
        begrunnelseOnChange={(e) => setBegrunnelseMindreBetalt(e.target.value)}
        belopUtbetaling={utbetalesTotal()}
        belopInnsendt={utbetaling.belop}
      />
      <AarsakerOgForklaringModal<"ANNET">
        open={avbrytModalOpen}
        header="Avbryt utbetaling"
        buttonLabel="Avbryt utbetaling"
        aarsaker={[{ value: "ANNET", label: "Annet" }]}
        onClose={() => setAvbrytModalOpen(false)}
        onConfirm={({ aarsaker, forklaring }) => {
          avbryt(aarsaker, forklaring);
        }}
      />
    </>
  );
}

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}

function MindreBelopModal({
  open,
  handleClose,
  onConfirm,
  belopInnsendt,
  belopUtbetaling,
  begrunnelseOnChange,
}: {
  open: boolean;
  handleClose: () => void;
  onConfirm: () => void;
  belopInnsendt: number;
  belopUtbetaling: number;
  begrunnelseOnChange: ChangeEventHandler<HTMLTextAreaElement>;
}) {
  return (
    <Modal
      open={open}
      className="text-left"
      onClose={handleClose}
      header={{
        heading: "Beløpet er mindre enn innsendt",
        icon: <InformationSquareFillIcon />,
      }}
    >
      <Modal.Body>
        <VStack gap="2">
          <VStack>
            <BodyShort>
              Beløpet du er i ferd med å sende til attestering er mindre en beløpet på utbetalingen.
              Er du sikker?
            </BodyShort>
            <BodyShort>Beløp til attestering: {formaterNOK(belopUtbetaling)}</BodyShort>
            <BodyShort>Innsendt beløp: {formaterNOK(belopInnsendt)}</BodyShort>
          </VStack>
          <Textarea
            label="Begrunnelse"
            onChange={begrunnelseOnChange}
            description="Oppgi begrunnelse for beløp som utbetales. Hvis det er benyttet flere tilsagn, begrunner du hvordan fordelingen er gjort. Begrunnelsen vil kun være synlig for NAV"
          />
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="primary" onClick={onConfirm}>
          Ja, send til attestering
        </Button>
        <Button variant="secondary" onClick={handleClose}>
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
