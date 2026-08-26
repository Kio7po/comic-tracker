import { useEffect, useRef, useState } from 'react';
import { cn } from '@/common/lib/utils';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/common/components/ui/tooltip';

interface TruncatedTextProps {
  text: string;
  className?: string;
}

function TruncatedText({ text, className }: Readonly<TruncatedTextProps>) {
  const textRef = useRef<HTMLParagraphElement>(null);
  const [isTruncated, setIsTruncated] = useState(false);

  useEffect(() => {
    const textElement = textRef.current;
    if (!textElement) return;

    const checkTruncation = () => {
      setIsTruncated(textElement.scrollWidth > textElement.clientWidth);
    };

    checkTruncation();
    const resizeObserver = new ResizeObserver(checkTruncation);
    resizeObserver.observe(textElement);
    return () => resizeObserver.disconnect();
  }, []);

  return (
    <Tooltip disabled={!isTruncated}>
      <TooltipTrigger render={<p ref={textRef} className={cn('truncate text-left', className)} />}>
        {text}
      </TooltipTrigger>
      <TooltipContent>{text}</TooltipContent>
    </Tooltip>
  );
}

export default TruncatedText;
