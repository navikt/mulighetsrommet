import { FormButtons } from "@/components/skjema/FormButtons";

interface Props {
  redigeringsModus: boolean;
  onClose: () => void;
  isPending: boolean;
}

export function GjennomforingFormKnapperad({ redigeringsModus, onClose, isPending }: Props) {
  return (
    <FormButtons
      onCancel={onClose}
      isPending={isPending}
      submitLabel={isPending ? "Lagrer..." : redigeringsModus ? "Lagre gjennomføring" : "Opprett"}
    />
  );
}
