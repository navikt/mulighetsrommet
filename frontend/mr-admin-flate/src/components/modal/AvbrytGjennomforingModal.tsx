import { AnnetEnum } from "@/api/annetEnum";
import { useAvbrytGjennomforing } from "@/api/gjennomforing/useAvbrytGjennomforing";
import { useSuspenseGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { Laster } from "@/components/laster/Laster";
import { AvbrytModalAarsaker } from "@/components/modal/AvbrytModalAarsaker";
import { AvbrytModalError } from "@/components/modal/AvbrytModalError";
import { AvbrytGjennomforingAarsak, GjennomforingDto, ProblemDetail } from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { Alert, BodyShort, Button, Radio } from "@navikt/ds-react";
import { RefObject, useState } from "react";
import { useNavigate } from "react-router";
import z from "zod";

export const AvbrytGjennomforingModalSchema = z.object({
  aarsak: z.enum({ ...AvbrytGjennomforingAarsak, ...AnnetEnum }, { error: "Mangler årsak" }),
  customAarsak: z.string().max(100, "Beskrivelse kan ikke inneholde mer enn 100 tegn").optional(),
});

interface Props {
  modalRef: RefObject<HTMLDialogElement | null>;
  gjennomforing: GjennomforingDto;
}

interface State {
  aarsak?: AvbrytGjennomforingAarsak | AnnetEnum;
  customAarsak?: string;
  errors: { aarsakError?: string; customAarsakError?: string };
}

const initialState: State = {
  aarsak: undefined,
  customAarsak: undefined,
  errors: {},
};

export const AvbrytGjennomforingModal = ({ modalRef, gjennomforing }: Props) => {
  const mutation = useAvbrytGjennomforing();
  const navigate = useNavigate();
  const { data: deltakerSummary } = useSuspenseGjennomforingDeltakerSummary(gjennomforing.id);
  const [state, setState] = useState<State>(initialState);
  const [error, setError] = useState<string | undefined>(undefined);

  const onClose = () => {
    setState(initialState);
    mutation.reset();
    modalRef.current?.close();
  };

  function onSuccess() {
    setState(initialState);
    modalRef.current?.close();
    navigate(`/gjennomforinger/${gjennomforing?.id}`);
  }

  function onError(error: ProblemDetail) {
    setError(error.detail);
  }

  const handleAvbrytGjennomforing = () => {
    const parsed = AvbrytGjennomforingModalSchema.safeParse({
      aarsak: state?.aarsak,
      customAarsak: state?.customAarsak,
    });
    if (!parsed.success) {
      const aarsakErrors = parsed.error.format();
      setState({
        ...state,
        errors: {
          aarsakError: aarsakErrors.aarsak?._errors.join("\n"),
          customAarsakError: aarsakErrors.customAarsak?._errors.join("\n"),
        },
      });
    }

    if (parsed.success && gjennomforing?.id && state?.aarsak) {
      if (state.aarsak === AnnetEnum.ANNET && state.customAarsak) {
        mutation.mutate(
          {
            id: gjennomforing.id,
            aarsak: state.customAarsak,
          },
          { onSuccess, onError },
        );
      } else
        mutation.mutate(
          {
            id: gjennomforing?.id,
            aarsak: state.aarsak,
          },
          { onSuccess, onError },
        );
    }
  };

  const aarsakToString = (aarsak: AvbrytGjennomforingAarsak): string => {
    switch (aarsak) {
      case AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA:
        return "Avbrutt i Arena";
      case AvbrytGjennomforingAarsak.BUDSJETT_HENSYN:
        return "Budsjetthensyn";
      case AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR:
        return "Endring hos arrangør";
      case AvbrytGjennomforingAarsak.FEILREGISTRERING:
        return "Feilregistrering";
      case AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE:
        return "For få deltakere";
    }
  };

  return (
    <VarselModal
      modalRef={modalRef}
      handleClose={onClose}
      headingIconType="warning"
      headingText={
        error
          ? `Kan ikke avbryte «${gjennomforing?.navn}»`
          : `Ønsker du å avbryte «${gjennomforing?.navn}»?`
      }
      body={
        <>
          {deltakerSummary.antallDeltakere > 0 && (
            <Alert variant="warning">
              {`Det finnes ${deltakerSummary.antallDeltakere} deltaker${deltakerSummary.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
           avbryte denne vil det føre til statusendring på alle deltakere som har en aktiv status.`}
            </Alert>
          )}
          <AvbrytModalAarsaker
            aarsak={state?.aarsak}
            customAarsak={state?.customAarsak}
            setAarsak={(aarsak) => {
              setState({
                ...state,
                aarsak: aarsak as AvbrytGjennomforingAarsak | AnnetEnum,
                errors: { ...state.errors, aarsakError: undefined },
              });
            }}
            setCustomAarsak={(customAarsak) => {
              setState({
                ...state,
                customAarsak,
                errors: { ...state.errors, customAarsakError: undefined },
              });
            }}
            aarsakError={state?.errors.aarsakError}
            customAarsakError={state?.errors.customAarsakError}
            radioknapp={
              <>
                {(Object.keys(AvbrytGjennomforingAarsak) as Array<AvbrytGjennomforingAarsak>)
                  .filter((a) => a !== AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA)
                  .map((a) => (
                    <Radio key={`${a}`} value={a}>
                      {aarsakToString(a)}
                    </Radio>
                  ))}
              </>
            }
          />
          {error && (
            <BodyShort>
              <AvbrytModalError
                aarsak={state.aarsak}
                customAarsak={state?.customAarsak}
                error={error}
              />
            </BodyShort>
          )}
        </>
      }
      secondaryButton
      primaryButton={
        <Button variant="danger" onClick={handleAvbrytGjennomforing} disabled={mutation.isPending}>
          {mutation.isPending ? <Laster /> : "Ja, jeg vil avbryte gjennomføringen"}
        </Button>
      }
    />
  );
};
