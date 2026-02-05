import { ReactNode } from "react";
import React from "react";

interface Props {
  separator?: boolean;
  children: ReactNode;
}

export function TwoColumnGrid(props: Props) {
  const [leftChild, ...rightChildren] = React.Children.toArray(props.children);
  const { separator = false } = props;

  return (
    <div className="grid grid-cols-1 ax-lg:grid-cols-2 h-full">
      <div className="mb-8 ax-lg:mb-0 ax-lg:pr-4 ax-md:pr-0">
        {/* Left Column Content */}
        {leftChild}
      </div>
      <div className="relative ax-lg:pl-4 ax-md:pl-0">
        {/* Right Column Content */}
        {separator && (
          <div className="hidden ax-lg:block absolute inset-y-0 left-0 w-px bg-ax-neutral-400 ax-md:my-4"></div>
        )}
        {...rightChildren}
      </div>
    </div>
  );
}
