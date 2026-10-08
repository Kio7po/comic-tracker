import { useState, type SyntheticEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { BookOpen, Library, PlusCircle, Search } from 'lucide-react';
import { Button } from '@/common/components/ui/button';
import { Card, CardDescription, CardHeader, CardTitle } from '@/common/components/ui/card';
import { Input } from '@/common/components/ui/input';
import ManganamaoLogo from '@/common/components/ManganamaoLogo';

function Home() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [keywords, setKeywords] = useState('');

  function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = keywords.trim();
    navigate(trimmed ? `/catalog?keywords=${encodeURIComponent(trimmed)}` : '/catalog');
  }

  const cards = [
    { to: '/catalog', icon: BookOpen, key: 'browse' },
    { to: '/library', icon: Library, key: 'library' },
    { to: '/catalog', icon: PlusCircle, key: 'contribute' },
  ] as const;

  const steps = t('home.steps', { returnObjects: true }) as { title: string; description: string }[];

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-16 px-4 py-12 sm:py-20">
      <section className="flex flex-col items-center gap-6 text-center">
        <ManganamaoLogo className="h-auto w-80 max-w-full sm:w-xl" />
        <h1 className="max-w-2xl text-balance text-3xl font-semibold tracking-tight sm:text-5xl">
          {t('home.title')}
        </h1>
        <p className="max-w-xl text-pretty text-muted-foreground sm:text-lg">{t('home.subtitle')}</p>
        <form onSubmit={handleSubmit} role="search" className="flex w-full max-w-xl gap-2">
          <div className="relative flex-1">
            <Search
              className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
            <Input
              type="search"
              value={keywords}
              onChange={(event) => setKeywords(event.target.value)}
              placeholder={t('home.searchPlaceholder')}
              aria-label={t('home.searchPlaceholder')}
              className="h-10 pl-9"
            />
          </div>
          <Button type="submit" className="h-10">
            {t('home.searchButton')}
          </Button>
        </form>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        {cards.map(({ to, icon: Icon, key }) => (
          <Link key={key} to={to} className="group focus-visible:outline-none">
            <Card className="h-full transition-colors group-hover:bg-muted/50 group-focus-visible:ring-3 group-focus-visible:ring-ring/50">
              <CardHeader>
                <Icon className="mb-2 size-5 text-primary" aria-hidden="true" />
                <CardTitle>{t(`home.${key}.title`)}</CardTitle>
                <CardDescription>{t(`home.${key}.description`)}</CardDescription>
              </CardHeader>
            </Card>
          </Link>
        ))}
      </section>

      <section className="flex flex-col gap-8">
        <h2 className="text-center text-2xl font-semibold tracking-tight">{t('home.howTitle')}</h2>
        <ol className="grid gap-6 sm:grid-cols-3">
          {steps.map((step, index) => (
            <li key={step.title} className="flex flex-col gap-2">
              <span className="flex size-8 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
                {index + 1}
              </span>
              <h3 className="font-medium">{step.title}</h3>
              <p className="text-sm text-muted-foreground">{step.description}</p>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

export default Home;
