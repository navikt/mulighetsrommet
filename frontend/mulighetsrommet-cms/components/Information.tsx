import React from "react";
import "./Information.scss";
import { InformationSquareIcon } from "@navikt/aksel-icons";

interface Props {
  melding?: string;
}

export const Information = ({
  melding = "Ikke del personopplysninger i fritekstfeltene",
}: Props) => {
  return (
    <div className="information">
      <InformationSquareIcon className="svg_info" />
      {melding}
    </div>
  );
};
