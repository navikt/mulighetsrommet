import React from "react";
import { InformationColored } from "@navikt/ds-icons";
import "./Information.scss";

export const Information = () => {
  return (
    <div className="information">
      <InformationColored /> Ikke del personopplysninger i fritekstfeltene.
    </div>
  );
};
