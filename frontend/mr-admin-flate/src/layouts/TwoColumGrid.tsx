import { ReactNode } from "react";
import React from "react";

interface Props {
  separator?: boolean;
  children: ReactNode;
}

export function TwoColumnGrid(props: Props) {
  const [leftChild, rightChild] = React.Children.toArray(props.children);
  const { separator = false } = props;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 h-full">
      <div className="mb-8 lg:mb-0 lg:pr-4 md:pr-0">
        {/* Left Column Content */}
        {leftChild}
      </div>
      <div className="relative lg:pl-4 md:pl-0">
        {/* Right Column Content */}
        {separator && (
          <div className="hidden lg:block absolute inset-y-0 left-0 w-px bg-gray-300 md:my-4"></div>
        )}
        {rightChild}
      </div>
    </div>
  );
}
