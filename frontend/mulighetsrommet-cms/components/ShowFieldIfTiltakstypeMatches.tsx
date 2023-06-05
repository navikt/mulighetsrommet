import React, { useEffect, useState } from "react";
import { useClient, useFormValue } from "sanity";
import { API_VERSION } from "../sanity.config";
export function ShowFieldIfTiltakstypeMatches(props: any, tiltakstype: string) {
  const client = useClient({ apiVersion: API_VERSION });
  const [visKomponent, setVisKomponent] = useState(false);
  const tiltakstypeRef = useFormValue(["tiltakstype"])?._ref;

  useEffect(() => {
    if (!tiltakstypeRef) {
      console.warn(
        "Du prøver å bruke komponenten på et skjema som ikke refererer til tiltakstype. Det går ikke."
      );
      return;
    }

    const hentTiltakstype = async () => {
      const res = await client.fetch(
        "*[_type == 'tiltakstype' && _id == $ref][0]",
        { ref: tiltakstypeRef }
      );

      setVisKomponent(res.tiltakstypeNavn === tiltakstype);
    };
    hentTiltakstype();
  }, [client, tiltakstypeRef, tiltakstype]);

  return visKomponent ? props.renderDefault(props) : null;
}
