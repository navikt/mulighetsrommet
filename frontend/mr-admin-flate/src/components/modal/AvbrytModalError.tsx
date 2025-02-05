import { AvbrytAvtaleAarsak, AvbrytGjennomforingAarsak } from "@mr/api-client-v2";
import { AnnetEnum } from "@/api/annetEnum";

interface Props {
  error?: string;
  aarsak?: AvbrytAvtaleAarsak | AvbrytGjennomforingAarsak | AnnetEnum;
  customAarsak?: string;
}
export function AvbrytModalError({ error, aarsak, customAarsak }: Props) {
  const beskrivelser = () => {
    if (aarsak?.length === 0) return "Du må velge en årsak";
    else if (aarsak === "annet" && !customAarsak)
      return "Beskrivelse er obligatorisk når “Annet” er valgt som årsak";
    else if (aarsak === "annet" && customAarsak!.length > 100)
      return "Beskrivelse kan ikke inneholde mer enn 100 tegn";
    return error;
  };

  return (
    <div className="text-red-500">
      <b>• {beskrivelser()}</b>
    </div>
  );
}
