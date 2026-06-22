import { Card } from '../../../components/Card'
import { useLanguage } from '../../../context/LanguageContext'
import type { ServiceEntity } from '../serviceModel'
import { getServicePrimaryImageUrl } from '../serviceModel'

interface ServiceCardProps {
  service: ServiceEntity
  onOpen: (service: ServiceEntity) => void
}

export function ServiceCard({ service, onOpen }: ServiceCardProps) {
  const { t } = useLanguage()
  return (
    <Card className="p-0 overflow-hidden">
      <button type="button" onClick={() => onOpen(service)} className="block w-full text-left">
        <img
          src={getServicePrimaryImageUrl(service)}
          alt={service.name}
          className="h-44 w-full object-cover"
          loading="lazy"
        />
        <div className="p-5">
          <h3 className="font-semibold text-slate-900 dark:text-slate-100">{service.name}</h3>
          <div className="mt-1 flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400">
            {service.rating != null ? (
              <>
                {Array.from({ length: 5 }, (_, i) => (
                  <span key={i} className={i < Math.round(Number(service.rating)) ? 'text-amber-400' : 'text-slate-300 dark:text-slate-600'} aria-hidden>
                    ★
                  </span>
                ))}
                <span className="ml-1 text-xs">{Number(service.rating).toFixed(1)}</span>
              </>
            ) : (
              t('servicesPage.noRatingYet')
            )}
          </div>
          <p className="mt-2 line-clamp-2 text-sm text-slate-600 dark:text-slate-300">
            {service.description || t('servicesPage.noDescription')}
          </p>
        </div>
      </button>
    </Card>
  )
}
