import { useState } from "react";
import { Sortering } from "../components/tabell/Types";

export const useSort = (
  sortKey: string,
  direction: "ascending" | "descending" = "ascending"
) => {
  return useState<Sortering>({
    orderBy: sortKey,
    direction,
  });
};
