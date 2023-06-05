import React, { useEffect, useState } from "react";
import { useClient, useFormValue } from "sanity";
import { API_VERSION } from "../sanity.config";
export function ShowFieldIfTiltakstypeMatches(props: any, tiltakstype: string) {
  const client = useClient({ apiVersion: API_VERSION });
  console.log(props);
  const [visKomponent, setVisKomponent] = useState(false);
  const tiltakstypeRef = useFormValue(["tiltakstype"])?._ref;

  useEffect(() => {
    if (!tiltakstypeRef) return;

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
