import type { ComponentProps } from 'react';

function ExternalLink({ children, ...props }: Readonly<ComponentProps<'a'>>) {
  return (
    <a target="_blank" rel="noopener noreferrer" {...props}>
      {children}
    </a>
  );
}

export default ExternalLink;