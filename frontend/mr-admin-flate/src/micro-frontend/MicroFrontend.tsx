import React, { useEffect } from "react";
import { importMicroFrontend } from "@/micro-frontend/import-micro-frontend";

export interface MicroFrontendProps {
  baseUrl: string;
  customComponentName: string;
  customComponentProps?: { [key: string]: string };
}

export function MicroFrontend(props: MicroFrontendProps) {
  useEffect(() => {
    importMicroFrontend(props.baseUrl);
  }, [props.baseUrl]);

  return React.createElement(props.customComponentName, props.customComponentProps);
}
