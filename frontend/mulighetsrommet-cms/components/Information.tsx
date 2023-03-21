import React from "react";
import "./Information.scss";
import { InformationSquareIcon } from "@navikt/aksel-icons";

export const Information = () => {
  return (
    <div className="information">
      <InformationSquareIcon className="svg_info" />
      Ikke del personopplysninger i fritekstfeltene.
    </div>
  );
};
